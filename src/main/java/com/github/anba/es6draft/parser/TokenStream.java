/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import static com.github.anba.es6draft.parser.NumberParser.parseBinary;
import static com.github.anba.es6draft.parser.NumberParser.parseDecimal;
import static com.github.anba.es6draft.parser.NumberParser.parseHex;
import static com.github.anba.es6draft.parser.NumberParser.parseOctal;

import java.util.Arrays;

import com.github.anba.es6draft.parser.ParserException.ExceptionType;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;

/**
 * Lexer for ECMAScript 6 source code
 */
public final class TokenStream {
    private static final boolean DEBUG = false;

    private final Parser parser;
    private final TokenStreamInput input;

    /** current line number */
    private int line;
    /** start position of current line */
    private int linestart;
    /** start position of current token, includes leading whitespace and comments */
    private int position;
    /** start position of next token, includes leading whitespace and comments */
    private int nextposition;

    // token data
    /** current token in stream */
    private Token current;
    /** next token in stream */
    private Token next;
    /** line terminator preceding current token? */
    private boolean hasCurrentLineTerminator;
    /** line terminator preceding next token? */
    private boolean hasLineTerminator;
    /** start line/column info for current token */
    private long sourcePosition;
    /** start line/column info for next token */
    private long nextSourcePosition;

    // literal data
    private StringBuffer buffer = new StringBuffer();
    private String string = null;
    private double number = 0;
    private boolean hasEscape = false;

    private static final class StringBuffer {
        char[] cbuf = new char[512];
        int length = 0;

        void clear() {
            length = 0;
        }

        void add(int c) {
            int len = length;
            if (len == cbuf.length) {
                cbuf = Arrays.copyOf(cbuf, len << 1);
            }
            cbuf[len] = (char) c;
            length = len + 1;
        }

        void addCodepoint(int c) {
            if (c > 0xFFFF) {
                add(Character.highSurrogate(c));
                add(Character.lowSurrogate(c));
            } else {
                add(c);
            }
        }

        void add(String s) {
            int len = length;
            int newlen = len + s.length();
            if (newlen > cbuf.length) {
                cbuf = Arrays.copyOf(cbuf, Integer.highestOneBit(newlen) << 1);
            }
            s.getChars(0, s.length(), cbuf, len);
            length = newlen;
        }

        @Override
        public String toString() {
            return new String(cbuf, 0, length);
        }
    }

    private StringBuffer buffer() {
        StringBuffer buffer = this.buffer;
        buffer.clear();
        return buffer;
    }

    private void incrementLine() {
        line += 1;
        linestart = input.position();
    }

    private void incrementLineAndUpdate() {
        line += 1;
        linestart = input.position();
        hasLineTerminator = true;
    }

    private void updateSourcePosition() {
        nextSourcePosition = ((long) (input.position() - linestart) << 32) | line;
    }

    public TokenStream(Parser parser, TokenStreamInput input) {
        this.parser = parser;
        this.input = input;
    }

    public int position() {
        return position;
    }

    public String range(int from, int to) {
        return input.range(from, to);
    }

    public long lineinfo() {
        return ((long) line << 32) | linestart;
    }

    public long sourcePosition() {
        return sourcePosition;
    }

    public long beginPosition() {
        return sourcePosition;
    }

    public long endPosition() {
        // add one to make columns 1-indexed
        return ((long) (1 + position - linestart) << 32) | line;
    }

    public TokenStream initialise() {
        // set internal state to default values
        this.hasLineTerminator = true;
        this.hasCurrentLineTerminator = true;
        this.position = input.position();
        this.line = parser.getSourceLine();
        this.linestart = input.position();
        this.current = scanTokenNoComment();
        this.sourcePosition = nextSourcePosition;
        this.nextposition = input.position();
        this.next = null;
        return this;
    }

    public void reset(long position, long lineinfo) {
        // reset character stream
        input.reset((int) position);
        // reset internal state
        this.hasLineTerminator = false;
        this.hasCurrentLineTerminator = true;
        this.position = input.position();
        this.current = scanTokenNoComment();
        this.sourcePosition = nextSourcePosition;
        this.nextposition = input.position();
        this.next = null;
        // reset line state last, effectively ignoring any changes from scanTokenNoComment()
        this.line = (int) (lineinfo >>> 32);
        this.linestart = (int) lineinfo;
    }

    public String getString() {
        if (string == null) {
            string = buffer.toString();
        }
        return string;
    }

    public boolean hasEscape() {
        return hasEscape;
    }

    public double getNumber() {
        return number;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return input.position() - linestart;
    }

    //

    public Token nextToken() {
        if (next == null) {
            hasLineTerminator = false;
            nextposition = input.position();
            next = scanTokenNoComment();
        }
        current = next;
        sourcePosition = nextSourcePosition;
        position = nextposition;
        hasCurrentLineTerminator = hasLineTerminator;
        string = null;
        next = null;
        nextposition = input.position();
        hasLineTerminator = false;
        return current;
    }

    public Token currentToken() {
        return current;
    }

    public Token peekToken() {
        assert !(current == Token.DIV || current == Token.ASSIGN_DIV);
        if (next == null) {
            if (current == Token.NAME || current == Token.STRING) {
                string = getString();
            }
            hasLineTerminator = false;
            nextposition = input.position();
            next = scanTokenNoComment();
        }
        return next;
    }

    public boolean hasCurrentLineTerminator() {
        assert current != null;
        return hasCurrentLineTerminator;
    }

    public boolean hasNextLineTerminator() {
        assert next != null;
        return hasLineTerminator;
    }

    //

    /**
     * <strong>[11.8.5] Regular Expression Literals</strong>
     * 
     * <pre>
     * RegularExpressionLiteral ::
     *     / RegularExpressionBody / RegularExpressionFlags
     * RegularExpressionBody ::
     *     RegularExpressionFirstChar RegularExpressionChars
     * RegularExpressionChars ::
     *     [empty]
     *     RegularExpressionChars RegularExpressionChar
     * RegularExpressionFirstChar ::
     *     RegularExpressionNonTerminator but not one of * or \ or / or [
     *     RegularExpressionBackslashSequence
     *     RegularExpressionClass
     * RegularExpressionChar ::
     *     RegularExpressionNonTerminator but not one of \ or / or [
     *     RegularExpressionBackslashSequence
     *     RegularExpressionClass
     * RegularExpressionBackslashSequence ::
     *     \ RegularExpressionNonTerminator
     * RegularExpressionNonTerminator ::
     *     SourceCharacter but not LineTerminator
     * RegularExpressionClass ::
     *     [ RegularExpressionClassChars ]
     * RegularExpressionClassChars ::
     *     [empty]
     *     RegularExpressionClassChars RegularExpressionClassChar
     * RegularExpressionClassChar ::
     *     RegularExpressionNonTerminator but not one of ] or \
     *     RegularExpressionBackslashSequence
     * RegularExpressionFlags ::
     *     [empty]
     *     RegularExpressionFlags IdentifierPart
     * </pre>
     */
    public String[] readRegularExpression(Token start) {
        assert start == Token.DIV || start == Token.ASSIGN_DIV;
        assert next == null : "regular expression in lookahead";

        final int EOF = TokenStreamInput.EOF;
        TokenStreamInput input = this.input;
        StringBuffer buffer = buffer();
        if (start == Token.ASSIGN_DIV) {
            buffer.add('=');
        } else {
            int c = input.peek(0);
            if (c == '/' || c == '*') {
                throw error(Messages.Key.InvalidRegExpLiteral);
            }
        }
        boolean inClass = false;
        for (;;) {
            int c = input.get();
            if (c == '\\') {
                // escape sequence
                buffer.add(c);
                c = input.get();
            } else if (c == '[') {
                inClass = true;
            } else if (c == ']') {
                inClass = false;
            } else if (c == '/' && !inClass) {
                break;
            }
            if (c == EOF || isLineTerminator(c)) {
                throw error(Messages.Key.UnterminatedRegExpLiteral);
            }
            buffer.add(c);
        }
        String regexp = buffer.toString();

        buffer.clear();
        for (;;) {
            int c = input.get();
            if (!isIdentifierPart(c)) {
                if (c == '\\' && match('u')) {
                    readUnicode();
                    throw error(Messages.Key.UnicodeEscapeInRegExpFlags);
                }
                input.unget(c);
                break;
            }
            buffer.add(c);
        }

        String flags = buffer.toString();
        return new String[] { regexp, flags };
    }

    //

    /**
     * <strong>[11.8.6] Template Literal Lexical Components</strong>
     * 
     * <pre>
     * Template ::
     *     NoSubstitutionTemplate 
     *     TemplateHead
     * NoSubstitutionTemplate ::
     *     ` TemplateCharacters<sub>opt</sub>`
     * TemplateHead ::
     *     ` TemplateCharacters<sub>opt</sub>${
     * TemplateSubstitutionTail ::
     *     TemplateMiddle 
     *     TemplateTail
     * TemplateMiddle ::
     *     } TemplateCharacters<sub>opt</sub>${
     * TemplateTail ::
     *     } TemplateCharacters<sub>opt</sub>`
     * TemplateCharacters ::
     *     TemplateCharacter TemplateCharacters<sub>opt</sub>
     * TemplateCharacter ::
     *     SourceCharacter but not one of ` or \ or $ 
     *     $ [LA &#x2209; { ]
     *     \ EscapeSequence
     *     LineContinuation
     * </pre>
     */
    public String[] readTemplateLiteral(Token start) {
        assert start == Token.TEMPLATE || start == Token.RC;
        assert currentToken() == start;
        assert next == null : "template literal in lookahead";

        final int EOF = TokenStreamInput.EOF;
        TokenStreamInput input = this.input;
        StringBuilder raw = new StringBuilder();
        StringBuffer buffer = buffer();
        int pos = input.position();
        for (;;) {
            int c = input.get();
            if (c == EOF) {
                throw eofError(Messages.Key.UnterminatedTemplateLiteral);
            }
            if (c == '`') {
                current = Token.TEMPLATE;
                raw.append(input.range(pos, input.position() - 1));
                return new String[] { buffer.toString(), raw.toString() };
            }
            if (c == '$' && match('{')) {
                current = Token.LC;
                raw.append(input.range(pos, input.position() - 2));
                return new String[] { buffer.toString(), raw.toString() };
            }
            if (c != '\\') {
                if (isLineTerminator(c)) {
                    // line terminator sequence
                    if (c == '\r') {
                        // normalise \r and \r\n to \n
                        raw.append(input.range(pos, input.position() - 1)).append('\n');
                        match('\n');
                        pos = input.position();
                        c = '\n';
                    }
                    buffer.add(c);
                    incrementLine();
                    continue;
                }
                // TODO: add substring range
                buffer.add(c);
                continue;
            }

            c = input.get();
            if (c == EOF) {
                throw eofError(Messages.Key.UnterminatedTemplateLiteral);
            }
            // EscapeSequence
            if (isLineTerminator(c)) {
                // line continuation
                if (c == '\r') {
                    // normalise \r and \r\n to \n
                    raw.append(input.range(pos, input.position() - 1)).append('\n');
                    match('\n');
                    pos = input.position();
                }
                incrementLine();
                continue;
            }
            switch (c) {
            case 'b':
                c = '\b';
                break;
            case 'f':
                c = '\f';
                break;
            case 'n':
                c = '\n';
                break;
            case 'r':
                c = '\r';
                break;
            case 't':
                c = '\t';
                break;
            case 'v':
                c = '\u000B';
                break;
            case '0':
                if (isDecimalDigit(input.peek(0))) {
                    throw error(Messages.Key.InvalidNULLEscape);
                }
                c = '\0';
                break;
            case 'x':
                c = (hexDigit(input.get()) << 4) | hexDigit(input.get());
                if (c < 0) {
                    throw error(Messages.Key.InvalidHexEscape);
                }
                break;
            case 'u':
                c = readUnicode();
                break;
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                throw error(Messages.Key.StrictModeOctalEscapeSequence);
            case '"':
            case '\'':
            case '\\':
            default:
                // fall-through
            }
            buffer.addCodepoint(c);
        }
    }

    //

    /**
     * <strong>[11] ECMAScript Language: Lexical Grammar</strong>
     * 
     * <pre>
     * InputElementDiv ::
     *     WhiteSpace
     *     LineTerminator
     *     Comment
     *     Token
     *     DivPunctuator
     *     RightBracePunctuator
     * InputElementRegExp ::
     *     WhiteSpace
     *     LineTerminator
     *     Comment
     *     Token
     *     RightBracePunctuator
     *     RegularExpressionLiteral
     * InputElementTemplateTail ::
     *     WhiteSpace
     *     LineTerminator
     *     Comment
     *     Token
     *     DivPunctuator
     *     TemplateSubstitutionTail
     * </pre>
     */
    private Token scanTokenNoComment() {
        Token tok;
        do {
            tok = scanToken();
        } while (tok == Token.COMMENT);
        return tok;
    }

    /**
     * <strong>[11.5] Token</strong>
     * 
     * <pre>
     * Token ::
     *     IdentifierName
     *     Punctuator
     *     NumericLiteral
     *     StringLiteral
     *     Template
     * </pre>
     */
    private Token scanToken() {
        TokenStreamInput input = this.input;

        int c;
        for (;;) {
            c = input.get();
            if (c == TokenStreamInput.EOF) {
                return Token.EOF;
            } else if (c <= 0x20) {
                if (c == 0x09 || c == 0x0B || c == 0x0C || c == 0x20) {
                    // skip over whitespace
                    continue;
                }
                if (c == '\n') {
                    incrementLineAndUpdate();
                    continue;
                }
                if (c == '\r') {
                    match('\n');
                    incrementLineAndUpdate();
                    continue;
                }
            } else if (c >= 0xA0) {
                if (isWhitespace(c)) {
                    // skip over whitespace
                    continue;
                }
                if (isLineTerminator(c)) {
                    incrementLineAndUpdate();
                    continue;
                }
            }
            break;
        }
        updateSourcePosition();

        if (DEBUG)
            System.out.printf("scanToken() -> %c\n", (char) c);

        switch (c) {
        case '\'':
        case '"':
            return readString(c);
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            return readNumberLiteral(c);
        case 'A':
        case 'B':
        case 'C':
        case 'D':
        case 'E':
        case 'F':
        case 'G':
        case 'H':
        case 'I':
        case 'J':
        case 'K':
        case 'L':
        case 'M':
        case 'N':
        case 'O':
        case 'P':
        case 'Q':
        case 'R':
        case 'S':
        case 'T':
        case 'U':
        case 'V':
        case 'W':
        case 'X':
        case 'Y':
        case 'Z':
        case 'a':
        case 'b':
        case 'c':
        case 'd':
        case 'e':
        case 'f':
        case 'g':
        case 'h':
        case 'i':
        case 'j':
        case 'k':
        case 'l':
        case 'm':
        case 'n':
        case 'o':
        case 'p':
        case 'q':
        case 'r':
        case 's':
        case 't':
        case 'u':
        case 'v':
        case 'w':
        case 'x':
        case 'y':
        case 'z':
        case '$':
        case '_':
            return readIdentifier(c);
        case '{':
            return Token.LC;
        case '}':
            return Token.RC;
        case '(':
            return Token.LP;
        case ')':
            return Token.RP;
        case '[':
            return Token.LB;
        case ']':
            return Token.RB;
        case '.':
            switch (input.peek(0)) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return readNumberLiteral(c);
            case '.':
                if (input.peek(1) == '.') {
                    mustMatch('.');
                    mustMatch('.');
                    return Token.TRIPLE_DOT;
                }
            }
            return Token.DOT;
        case ';':
            return Token.SEMI;
        case ',':
            return Token.COMMA;
        case '~':
            return Token.BITNOT;
        case '?':
            return Token.HOOK;
        case ':':
            return Token.COLON;
        case '<':
            if (match('<')) {
                if (match('=')) {
                    return Token.ASSIGN_SHL;
                } else {
                    return Token.SHL;
                }
            } else if (match('=')) {
                return Token.LE;
            } else if (input.peek(0) == '!' && input.peek(1) == '-' && input.peek(2) == '-'
                    && parser.isEnabled(CompatibilityOption.HTMLComments)) {
                // html start-comment
                mustMatch('!');
                mustMatch('-');
                mustMatch('-');
                readSingleComment();
                return Token.COMMENT;
            } else {
                return Token.LT;
            }
        case '>':
            if (match('>')) {
                if (match('>')) {
                    if (match('=')) {
                        return Token.ASSIGN_USHR;
                    } else {
                        return Token.USHR;
                    }
                } else if (match('=')) {
                    return Token.ASSIGN_SHR;
                } else {
                    return Token.SHR;
                }
            } else if (match('=')) {
                return Token.GE;
            } else {
                return Token.GT;
            }
        case '=':
            if (match('=')) {
                if (match('=')) {
                    return Token.SHEQ;
                } else {
                    return Token.EQ;
                }
            } else if (match('>')) {
                return Token.ARROW;
            } else {
                return Token.ASSIGN;
            }
        case '!':
            if (match('=')) {
                if (match('=')) {
                    return Token.SHNE;
                } else {
                    return Token.NE;
                }
            } else {
                return Token.NOT;
            }
        case '+':
            if (match('+')) {
                return Token.INC;
            } else if (match('=')) {
                return Token.ASSIGN_ADD;
            } else {
                return Token.ADD;
            }
        case '-':
            if (match('-')) {
                if (input.peek(0) == '>' && hasLineTerminator
                        && parser.isEnabled(CompatibilityOption.HTMLComments)) {
                    // html end-comment at line start
                    mustMatch('>');
                    readSingleComment();
                    return Token.COMMENT;
                }
                return Token.DEC;
            } else if (match('=')) {
                return Token.ASSIGN_SUB;
            } else {
                return Token.SUB;
            }
        case '*':
            if (match('=')) {
                return Token.ASSIGN_MUL;
            } else {
                return Token.MUL;
            }
        case '%':
            if (match('=')) {
                return Token.ASSIGN_MOD;
            } else {
                return Token.MOD;
            }
        case '/':
            if (match('=')) {
                return Token.ASSIGN_DIV;
            } else if (match('/')) {
                readSingleComment();
                return Token.COMMENT;
            } else if (match('*')) {
                readMultiComment();
                return Token.COMMENT;
            } else {
                return Token.DIV;
            }
        case '&':
            if (match('&')) {
                return Token.AND;
            } else if (match('=')) {
                return Token.ASSIGN_BITAND;
            } else {
                return Token.BITAND;
            }
        case '|':
            if (match('|')) {
                return Token.OR;
            } else if (match('=')) {
                return Token.ASSIGN_BITOR;
            } else {
                return Token.BITOR;
            }
        case '^':
            if (match('=')) {
                return Token.ASSIGN_BITXOR;
            } else {
                return Token.BITXOR;
            }
        case '`':
            return Token.TEMPLATE;
        }

        if (c == '\\') {
            mustMatch('u');
            c = readUnicode();
        }
        if (isIdentifierStart(c)) {
            return readIdentifier(c);
        }

        return Token.ERROR;
    }

    /**
     * <strong>[11.6] Identifier Names and Identifiers</strong>
     * 
     * <pre>
     * IdentifierStart ::
     *     UnicodeIDStart
     *     $
     *     _
     *     \ UnicodeEscapeSequence
     * UnicodeIDStart ::
     *     any Unicode character with the Unicode property “ID_Start”.
     * </pre>
     */
    private static boolean isIdentifierStart(int c) {
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '$' || c == '_')
            return true;
        if (c <= 127)
            return false;
        // cf. http://www.unicode.org/reports/tr31/ for definition of "UnicodeIDStart"
        if (c == '\u2E2F') {
            // VERTICAL TILDE is in 'Lm' and [:Pattern_Syntax:]
            return false;
        }
        switch (Character.getType(c)) {
        case Character.UPPERCASE_LETTER:
        case Character.LOWERCASE_LETTER:
        case Character.TITLECASE_LETTER:
        case Character.MODIFIER_LETTER:
        case Character.OTHER_LETTER:
        case Character.LETTER_NUMBER:
            return true;
        }
        return false;
    }

    /**
     * <strong>[11.6] Identifier Names and Identifiers</strong>
     * 
     * <pre>
     * IdentifierPart ::
     *     UnicodeIDContinue
     *     $
     *     _
     *     \ UnicodeEscapeSequence 
     *     &lt;ZWNJ&gt;
     *     &lt;ZWJ&gt;
     * UnicodeIDContinue ::
     *     any Unicode character with the Unicode property “ID_Continue”
     * </pre>
     */
    private static boolean isIdentifierPart(int c) {
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '$'
                || c == '_')
            return true;
        if (c <= 127)
            return false;
        if (c == '\u200C' || c == '\u200D')
            return true;
        // cf. http://www.unicode.org/reports/tr31/ for definition of "UnicodeIDContinue"
        if (c == '\u2E2F') {
            // VERTICAL TILDE is in 'Lm' and [:Pattern_Syntax:]
            return false;
        }
        switch (Character.getType(c)) {
        case Character.UPPERCASE_LETTER:
        case Character.LOWERCASE_LETTER:
        case Character.TITLECASE_LETTER:
        case Character.MODIFIER_LETTER:
        case Character.OTHER_LETTER:
        case Character.LETTER_NUMBER:
        case Character.NON_SPACING_MARK:
        case Character.COMBINING_SPACING_MARK:
        case Character.DECIMAL_DIGIT_NUMBER:
        case Character.CONNECTOR_PUNCTUATION:
            return true;
        }
        return false;
    }

    /**
     * <strong>[11.2] White Space</strong>
     * 
     * <pre>
     * WhiteSpace ::
     *     &lt;TAB>  (U+0009)
     *     &lt;VT>   (U+000B)
     *     &lt;FF>   (U+000C)
     *     &lt;SP>   (U+0020)
     *     &lt;NBSP> (U+00A0)
     *     &lt;BOM>  (U+FEFF)
     *     &lt;USP>  ("Zs")
     * </pre>
     */
    private static boolean isWhitespace(int c) {
        return (c == 0x09 || c == 0x0B || c == 0x0C || c == 0x20 || c == 0xA0 || c == 0xFEFF || isSpaceSeparator(c));
    }

    /**
     * Unicode category "Zs" (space separator)
     */
    private static boolean isSpaceSeparator(int c) {
        return (c == 0x20 || c == 0xA0 || c == 0x1680 || c == 0x180E
                || (c >= 0x2000 && c <= 0x200A) || c == 0x202F || c == 0x205F || c == 0x3000);
    }

    /**
     * <strong>[11.3] Line Terminators</strong>
     * 
     * <pre>
     * LineTerminator ::
     *     &lt;LF> (U+000A)
     *     &lt;CR> (U+000D)
     *     &lt;LS> (U+2028)
     *     &lt;PS> (U+2029)
     * </pre>
     */
    private static boolean isLineTerminator(int c) {
        if ((c & ~0b0010_0000_0010_1111) != 0) {
            return false;
        }
        return (c == 0x0A || c == 0x0D || c == 0x2028 || c == 0x2029);
    }

    /**
     * <strong>[11.4] Comments</strong>
     * 
     * <pre>
     * SingleLineComment ::
     *     // SingleLineCommentChars<sub>opt</sub>
     * SingleLineCommentChars ::
     *     SingleLineCommentChar SingleLineCommentChars<sub>opt</sub>
     * SingleLineCommentChar ::
     *     SourceCharacter but not LineTerminator
     * </pre>
     */
    private Token readSingleComment() {
        final int EOF = TokenStreamInput.EOF;
        TokenStreamInput input = this.input;
        for (;;) {
            int c = input.get();
            if (c == EOF) {
                break;
            }
            if (isLineTerminator(c)) {
                // EOL is not part of the single-line comment!
                input.unget(c);
                break;
            }
        }
        return Token.COMMENT;
    }

    /**
     * <strong>[11.4] Comments</strong>
     * 
     * <pre>
     * MultiLineComment ::
     *     /* MultiLineCommentChars<sub>opt</sub> &#42;/
     * MultiLineCommentChars ::
     *     MultiLineNotAsteriskChar MultiLineCommentChars<sub>opt</sub>
     *     PostAsteriskCommentChars<sub>opt</sub>
     * PostAsteriskCommentChars ::
     *     MultiLineNotForwardSlashOrAsteriskChar MultiLineCommentChars<sub>opt</sub>
     *     PostAsteriskCommentChars<sub>opt</sub>
     * MultiLineNotAsteriskChar ::
     *     SourceCharacter but not *
     * MultiLineNotForwardSlashOrAsteriskChar ::
     *     SourceCharacter but not one of / or *
     * </pre>
     */
    private Token readMultiComment() {
        final int EOF = TokenStreamInput.EOF;
        TokenStreamInput input = this.input;
        loop: for (;;) {
            int c = input.get();
            while (c == '*') {
                if ((c = input.get()) == '/')
                    break loop;
            }
            if (isLineTerminator(c)) {
                if (c == '\r') {
                    match('\n');
                }
                incrementLineAndUpdate();
            }
            if (c == EOF) {
                throw eofError(Messages.Key.UnterminatedComment);
            }
        }
        return Token.COMMENT;
    }

    /**
     * <strong>[11.6] Identifier Names and Identifiers</strong>
     * 
     * <pre>
     * Identifier ::
     *     IdentifierName but not ReservedWord
     * IdentifierName ::
     *     IdentifierStart
     *     IdentifierName IdentifierPart
     * </pre>
     */
    private Token readIdentifier(int c) {
        assert isIdentifierStart(c);

        TokenStreamInput input = this.input;
        StringBuffer buffer = this.buffer();
        buffer.addCodepoint(c);
        for (;;) {
            c = input.get();
            if (isIdentifierPart(c)) {
                buffer.add(c);
            } else if (c == '\\') {
                mustMatch('u');
                c = readUnicode();
                if (!isIdentifierPart(c)) {
                    throw error(Messages.Key.InvalidUnicodeEscapedIdentifierPart);
                }
                buffer.addCodepoint(c);
                continue;
            } else {
                input.unget(c);
                break;
            }
        }

        Token tok = readReservedWord(buffer);
        if (tok != null) {
            return tok;
        }
        return Token.NAME;
    }

    /**
     * <strong>[11.8.4] String Literals</strong>
     * 
     * <pre>
     * UnicodeEscapeSequence ::
     *     u HexDigit HexDigit HexDigit HexDigit
     *     u{ HexDigits }
     * </pre>
     */
    private int readUnicode() {
        TokenStreamInput input = this.input;
        int c = input.get();
        if (c == '{') {
            int acc = 0;
            c = input.get();
            do {
                acc = (acc << 4) | hexDigit(c);
            } while ((acc >= 0 && acc <= 0x10FFFF) && (c = input.get()) != '}');
            if (c == '}') {
                c = acc;
            } else {
                c = -1;
            }
        } else {
            c = (hexDigit(c) << 12) | (hexDigit(input.get()) << 8) | (hexDigit(input.get()) << 4)
                    | hexDigit(input.get());
        }
        if (c < 0 || c > 0x10FFFF) {
            throw error(Messages.Key.InvalidUnicodeEscape);
        }
        return c;
    }

    /**
     * <strong>[11.6.1] Reserved Words</strong>
     * 
     * <pre>
     * ReservedWord ::
     *     Keyword
     *     FutureReservedWord
     *     NullLiteral
     *     BooleanLiteral
     * </pre>
     * 
     * <strong>[11.6.1.1] Keywords</strong>
     * 
     * <pre>
     * Keyword :: one of
     *     break        delete      import      this
     *     case         do          in          throw
     *     catch        else        instanceof  try
     *     class        export      let         typeof
     *     continue     finally     new         var
     *     const        for         return      void
     *     debugger     function    super       while
     *     default      if          switch      with
     * </pre>
     * 
     * <strong>[11.6.1.2] Future Reserved Words</strong>
     * 
     * <pre>
     * FutureReservedWord :: one of
     *     enum         extends
     *     implements   private     public      yield
     *     interface    package     protected   static
     * </pre>
     * 
     * <strong>[11.8.1] Null Literals</strong>
     * 
     * <pre>
     * NullLiteral ::
     *     null
     * </pre>
     * 
     * <strong>[11.8.2] Boolean Literals</strong>
     * 
     * <pre>
     * BooleanLiteral ::
     *     true
     *     false
     * </pre>
     */
    private Token readReservedWord(StringBuffer buffer) {
        int length = buffer.length;
        if (length < 2 || length > 10)
            return null;
        char[] cbuf = buffer.cbuf;
        char c0 = cbuf[0], c1 = cbuf[1];
        Token test = null;
        switch (c0) {
        case 'b':
            // break
            if (length == 5)
                test = Token.BREAK;
            break;
        case 'c':
            // case, catch, continue, class, const
            if (length == 4)
                test = Token.CASE;
            else if (length == 5)
                test = (c1 == 'a' ? Token.CATCH : c1 == 'l' ? Token.CLASS : Token.CONST);
            else if (length == 8)
                test = Token.CONTINUE;
            break;
        case 'd':
            // debugger, default, delete, do
            if (length == 2)
                test = Token.DO;
            else if (length == 6)
                test = Token.DELETE;
            else if (length == 7)
                test = Token.DEFAULT;
            else if (length == 8)
                test = Token.DEBUGGER;
            break;
        case 'e':
            // else, enum, export, extends
            if (length == 4)
                test = (c1 == 'l' ? Token.ELSE : Token.ENUM);
            else if (length == 6)
                test = Token.EXPORT;
            else if (length == 7)
                test = Token.EXTENDS;
            break;
        case 'f':
            // finally, for, function, false
            if (length == 3)
                test = Token.FOR;
            else if (length == 5)
                test = Token.FALSE;
            else if (length == 7)
                test = Token.FINALLY;
            else if (length == 8)
                test = Token.FUNCTION;
            break;
        case 'i':
            // if, in, instanceof, import, implements, interface
            if (length == 2)
                test = (c1 == 'f' ? Token.IF : Token.IN);
            else if (length == 6)
                test = Token.IMPORT;
            else if (length == 9)
                test = Token.INTERFACE;
            else if (length == 10)
                test = (c1 == 'n' ? Token.INSTANCEOF : Token.IMPLEMENTS);
            break;
        case 'l':
            // let
            if (length == 3)
                test = Token.LET;
            break;
        case 'n':
            // new, null
            if (length == 3)
                test = Token.NEW;
            else if (length == 4)
                test = Token.NULL;
            break;
        case 'p':
            // package, private, protected, public
            if (length == 6)
                test = Token.PUBLIC;
            else if (length == 7)
                test = (c1 == 'a' ? Token.PACKAGE : Token.PRIVATE);
            else if (length == 9)
                test = Token.PROTECTED;
            break;
        case 'r':
            // return
            if (length == 6)
                test = Token.RETURN;
            break;
        case 's':
            // switch, super, static
            if (length == 5)
                test = Token.SUPER;
            else if (length == 6)
                test = (c1 == 'w' ? Token.SWITCH : Token.STATIC);
            break;
        case 't':
            // this, throw, try, typeof, true
            if (length == 3)
                test = Token.TRY;
            else if (length == 4)
                test = (c1 == 'h' ? Token.THIS : Token.TRUE);
            else if (length == 5)
                test = Token.THROW;
            else if (length == 6)
                test = Token.TYPEOF;
            break;
        case 'v':
            // var, void
            if (length == 3)
                test = Token.VAR;
            else if (length == 4)
                test = Token.VOID;
            break;
        case 'w':
            // while, with
            if (length == 4)
                test = Token.WITH;
            else if (length == 5)
                test = Token.WHILE;
            break;
        case 'y':
            // yield
            if (length == 5)
                test = Token.YIELD;
            break;
        }
        if (test != null && equals(cbuf, test.getName())) {
            return test;
        }
        return null;
    }

    private static boolean equals(char[] cbuf, String test) {
        for (int i = 0, length = test.length(); i < length; ++i) {
            if (cbuf[i] != test.charAt(i))
                return false;
        }
        return true;
    }

    /**
     * <strong>[11.8.4] String Literals</strong>
     * 
     * <pre>
     * StringLiteral ::
     *     " DoubleStringCharacters<sub>opt</sub> "
     *     ' SingleStringCharacters<sub>opt</sub> '
     * DoubleStringCharacters ::
     *     DoubleStringCharacter DoubleStringCharacters<sub>opt</sub>
     * SingleStringCharacters ::
     *     SingleStringCharacter SingleStringCharacters<sub>opt</sub>
     * DoubleStringCharacter ::
     *     SourceCharacter but not one of " or \ or LineTerminator
     *     \ EscapeSequence
     *     LineContinuation
     * SingleStringCharacter ::
     *     SourceCharacter but not one of ' or \ or LineTerminator
     *     \ EscapeSequence
     *     LineContinuation
     * LineContinuation ::
     *     \ LineTerminatorSequence
     * EscapeSequence ::
     *     CharacterEscapeSequence
     *     0  [lookahead &#x2209; DecimalDigit]
     *     HexEscapeSequence
     *     UnicodeEscapeSequence
     * CharacterEscapeSequence ::
     *     SingleEscapeCharacter
     *     NonEscapeCharacter
     * SingleEscapeCharacter ::  one of
     *     ' "  \  b f n r t v
     * NonEscapeCharacter ::
     *     SourceCharacter but not one of EscapeCharacter or LineTerminator
     * EscapeCharacter ::
     *     SingleEscapeCharacter
     *     DecimalDigit
     *     x
     *     u
     * HexEscapeSequence ::
     *     x HexDigit HexDigit
     * UnicodeEscapeSequence ::
     *     u HexDigit HexDigit HexDigit HexDigit
     *     u{ HexDigits }
     * </pre>
     * 
     * <strong>[B.1.2] String Literals</strong>
     * 
     * <pre>
     * EscapeSequence ::
     *     CharacterEscapeSequence
     *     OctalEscapeSequence
     *     HexEscapeSequence
     *     UnicodeEscapeSequence
     * </pre>
     */
    private Token readString(int quoteChar) {
        assert quoteChar == '"' || quoteChar == '\'';

        final int EOF = TokenStreamInput.EOF;
        TokenStreamInput input = this.input;
        int start = input.position();
        StringBuffer buffer = this.buffer();
        hasEscape = false;
        for (;;) {
            int c = input.get();
            if (c == EOF) {
                throw eofError(Messages.Key.UnterminatedStringLiteral);
            }
            if (c == quoteChar) {
                buffer.add(input.range(start, input.position() - 1));
                break;
            }
            if (isLineTerminator(c)) {
                throw error(Messages.Key.UnterminatedStringLiteral);
            }
            if (c != '\\') {
                continue;
            }
            buffer.add(input.range(start, input.position() - 1));
            hasEscape = true;
            c = input.get();
            if (isLineTerminator(c)) {
                // line continuation
                if (c == '\r' && match('\n')) {
                    // \r\n sequence
                }
                incrementLine();
                start = input.position();
                continue;
            }
            // escape sequences
            switch (c) {
            case 'b':
                c = '\b';
                break;
            case 'f':
                c = '\f';
                break;
            case 'n':
                c = '\n';
                break;
            case 'r':
                c = '\r';
                break;
            case 't':
                c = '\t';
                break;
            case 'v':
                c = '\u000B';
                break;
            case 'x':
                c = (hexDigit(input.get()) << 4) | hexDigit(input.get());
                if (c < 0) {
                    throw error(Messages.Key.InvalidHexEscape);
                }
                break;
            case 'u':
                c = readUnicode();
                break;
            case '0':
                if (isDecimalDigit(input.peek(0))) {
                    if (!parser.isEnabled(CompatibilityOption.OctalEscapeSequence)) {
                        throw error(Messages.Key.InvalidNULLEscape);
                    }
                    c = readOctalEscape(c);
                } else {
                    c = '\0';
                }
                break;
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
                if (!parser.isEnabled(CompatibilityOption.OctalEscapeSequence)) {
                    throw error(Messages.Key.StrictModeOctalEscapeSequence);
                }
                c = readOctalEscape(c);
                break;
            case '8':
            case '9':
                // FIXME: spec bug - undefined behaviour for \8 and \9
                if (!parser.isEnabled(CompatibilityOption.OctalEscapeSequence)) {
                    throw error(Messages.Key.StrictModeOctalEscapeSequence);
                }
                // fall-through
            case '"':
            case '\'':
            case '\\':
            default:
                // fall-through
            }
            buffer.addCodepoint(c);
            start = input.position();
        }

        return Token.STRING;
    }

    /**
     * <strong>[B.1.2] String Literals</strong>
     * 
     * <pre>
     * OctalEscapeSequence ::
     *     OctalDigit [lookahead &#x2209; DecimalDigit]
     *     ZeroToThree OctalDigit [lookahead &#x2209; DecimalDigit]
     *     FourToSeven OctalDigit
     *     ZeroToThree OctalDigit OctalDigit
     * ZeroToThree :: one of
     *     0 1 2 3
     * FourToSeven :: one of
     *     4 5 6 7
     * </pre>
     */
    private int readOctalEscape(int c) {
        parser.reportStrictModeSyntaxError(Messages.Key.StrictModeOctalEscapeSequence);
        int d = (c - '0');
        c = input.get();
        if (c < '0' || c > '7') {
            // FIXME: spec bug? behaviour for non-octal decimal digits?
            input.unget(c);
        } else {
            d = d * 8 + (c - '0');
            if (d <= 037) {
                c = input.get();
                if (c < '0' || c > '7') {
                    // FIXME: spec bug? behaviour for non-octal decimal digits?
                    input.unget(c);
                } else {
                    d = d * 8 + (c - '0');
                }
            }
        }
        return d;
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * NumericLiteral ::
     *     DecimalLiteral
     *     BinaryIntegerLiteral
     *     OctalIntegerLiteral
     *     HexIntegerLiteral
     * </pre>
     */
    private Token readNumberLiteral(int c) {
        if (c == '0') {
            int d = input.get();
            if (d == 'x' || d == 'X') {
                number = readHexIntegerLiteral();
            } else if (d == 'b' || d == 'B') {
                number = readBinaryIntegerLiteral();
            } else if (d == 'o' || d == 'O') {
                number = readOctalIntegerLiteral();
            } else if (isDecimalDigit(d)
                    && parser.isEnabled(CompatibilityOption.LegacyOctalIntegerLiteral)) {
                input.unget(d);
                number = readLegacyOctalIntegerLiteral();
            } else {
                input.unget(d);
                number = readDecimalLiteral(c);
            }
        } else {
            number = readDecimalLiteral(c);
        }
        return Token.NUMBER;
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * HexIntegerLiteral ::
     *     0x HexDigits
     *     0X HexDigits
     * HexDigits ::
     *     HexDigit
     *     HexDigits HexDigit
     * </pre>
     */
    private double readHexIntegerLiteral() {
        TokenStreamInput input = this.input;
        StringBuffer buffer = this.buffer();
        int c;
        while (isHexDigit(c = input.get())) {
            buffer.add(c);
        }
        if (isDecimalDigitOrIdentifierStart(c)) {
            throw error(Messages.Key.InvalidHexIntegerLiteral);
        }
        input.unget(c);
        if (buffer.length == 0) {
            throw error(Messages.Key.InvalidHexIntegerLiteral);
        }
        return parseHex(buffer.cbuf, buffer.length);
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * BinaryIntegerLiteral ::
     *     0b BinaryDigit
     *     0B BinaryDigit
     *     BinaryIntegerLiteral BinaryDigit
     * </pre>
     */
    private double readBinaryIntegerLiteral() {
        TokenStreamInput input = this.input;
        StringBuffer buffer = this.buffer();
        int c;
        while (isBinaryDigit(c = input.get())) {
            buffer.add(c);
        }
        if (isDecimalDigitOrIdentifierStart(c)) {
            throw error(Messages.Key.InvalidBinaryIntegerLiteral);
        }
        input.unget(c);
        if (buffer.length == 0) {
            throw error(Messages.Key.InvalidBinaryIntegerLiteral);
        }
        return parseBinary(buffer.cbuf, buffer.length);
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * OctalIntegerLiteral ::
     *     0o OctalDigit
     *     0O OctalDigit
     *     OctalIntegerLiteral OctalDigit
     * </pre>
     */
    private double readOctalIntegerLiteral() {
        TokenStreamInput input = this.input;
        StringBuffer buffer = this.buffer();
        int c;
        while (isOctalDigit(c = input.get())) {
            buffer.add(c);
        }
        if (isDecimalDigitOrIdentifierStart(c)) {
            throw error(Messages.Key.InvalidOctalIntegerLiteral);
        }
        input.unget(c);
        if (buffer.length == 0) {
            throw error(Messages.Key.InvalidOctalIntegerLiteral);
        }
        return parseOctal(buffer.cbuf, buffer.length);
    }

    /**
     * <strong>[B.1.1] Numeric Literals</strong>
     * 
     * <pre>
     * LegacyOctalIntegerLiteral ::
     *     0 OctalDigit
     *     LegacyOctalIntegerLiteral OctalDigit
     * </pre>
     */
    private double readLegacyOctalIntegerLiteral() {
        TokenStreamInput input = this.input;
        StringBuffer buffer = this.buffer();
        int c;
        while (isOctalDigit(c = input.get())) {
            buffer.add(c);
        }
        if (c == '8' || c == '9') {
            // invalid octal integer literal -> treat as decimal literal, no strict-mode error
            // FIXME: spec bug? undefined behaviour - SM reports a strict-mode error in this case
            return readDecimalLiteral(c, false);
        }
        parser.reportStrictModeSyntaxError(Messages.Key.StrictModeOctalIntegerLiteral);
        if (isDecimalDigitOrIdentifierStart(c)) {
            throw error(Messages.Key.InvalidOctalIntegerLiteral);
        }
        input.unget(c);
        if (buffer.length == 0) {
            throw error(Messages.Key.InvalidOctalIntegerLiteral);
        }
        return parseOctal(buffer.cbuf, buffer.length);
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * DecimalLiteral ::
     *     DecimalIntegerLiteral . DecimalDigits<sub>opt</sub> ExponentPart<sub>opt</sub>
     *     . DecimalDigits ExponentPart<sub>opt</sub>
     *     DecimalIntegerLiteral ExponentPart<sub>opt</sub>
     * DecimalIntegerLiteral ::
     *     0
     *     NonZeroDigit DecimalDigits<sub>opt</sub>
     * DecimalDigits ::
     *     DecimalDigit
     *     DecimalDigits DecimalDigit
     * NonZeroDigit :: one of
     *     1 2 3 4 5 6 7 8 9
     * ExponentPart ::
     *     ExponentIndicator SignedInteger
     * ExponentIndicator :: one of
     *     e E
     * SignedInteger ::
     *     DecimalDigits
     *     + DecimalDigits
     *     - DecimalDigits
     * </pre>
     */
    private double readDecimalLiteral(int c) {
        return readDecimalLiteral(c, true);
    }

    private double readDecimalLiteral(int c, boolean reset) {
        assert c == '.' || isDecimalDigit(c);
        TokenStreamInput input = this.input;
        StringBuffer buffer = reset ? this.buffer() : this.buffer;
        if (c != '.' && c != '0') {
            buffer.add(c);
            while (isDecimalDigit(c = input.get())) {
                buffer.add(c);
            }
        } else if (c == '0') {
            buffer.add(c);
            c = input.get();
        }
        if (c == '.') {
            buffer.add(c);
            while (isDecimalDigit(c = input.get())) {
                buffer.add(c);
            }
        }
        if (c == 'e' || c == 'E') {
            buffer.add(c);
            c = input.get();
            if (c == '+' || c == '-') {
                buffer.add(c);
                c = input.get();
            }
            if (!isDecimalDigit(c)) {
                throw error(Messages.Key.InvalidNumberLiteral);
            }
            buffer.add(c);
            while (isDecimalDigit(c = input.get())) {
                buffer.add(c);
            }
        }
        if (isDecimalDigitOrIdentifierStart(c)) {
            throw error(Messages.Key.InvalidNumberLiteral);
        }
        input.unget(c);
        return parseDecimal(buffer.cbuf, buffer.length);
    }

    private boolean isDecimalDigitOrIdentifierStart(int c) {
        return isDecimalDigit(c) || isIdentifierStart(c);
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * DecimalDigit :: one of
     *     0 1 2 3 4 5 6 7 8 9
     * </pre>
     */
    private static boolean isDecimalDigit(int c) {
        return (c >= '0' && c <= '9');
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * BinaryDigit :: one of
     *     0  1
     * </pre>
     */
    private static boolean isBinaryDigit(int c) {
        return (c == '0' || c == '1');
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * OctalDigit :: one of
     *     0  1  2  3  4  5  6  7
     * </pre>
     */
    private static boolean isOctalDigit(int c) {
        return (c >= '0' && c <= '7');
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * HexDigit :: one of
     *     0 1 2 3 4 5 6 7 8 9 a b c d e f A B C D E F
     * </pre>
     */
    private static boolean isHexDigit(int c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * HexDigit :: one of
     *     0 1 2 3 4 5 6 7 8 9 a b c d e f A B C D E F
     * </pre>
     */
    private static int hexDigit(int c) {
        if (c >= '0' && c <= '9') {
            return (c - '0');
        } else if (c >= 'A' && c <= 'F') {
            return (c - ('A' - 10));
        } else if (c >= 'a' && c <= 'f') {
            return (c - ('a' - 10));
        }
        return -1;
    }

    private ParserException error(Messages.Key messageKey, String... args) {
        throw new ParserException(ExceptionType.SyntaxError, parser.getSourceFile(), getLine(),
                getColumn(), messageKey, args);
    }

    private ParserException eofError(Messages.Key messageKey, String... args) {
        throw new ParserEOFException(parser.getSourceFile(), getLine(), getColumn(), messageKey,
                args);
    }

    private boolean match(char c) {
        return input.match(c);
    }

    private void mustMatch(char c) {
        if (input.get() != c) {
            throw error(Messages.Key.UnexpectedCharacter, String.valueOf((char) c));
        }
    }
}
