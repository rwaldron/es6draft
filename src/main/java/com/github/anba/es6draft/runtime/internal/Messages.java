/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 *
 */
public final class Messages {
    private static final PropertiesReaderControl UTF8_RESOURCE_CONTROL = new PropertiesReaderControl(
            StandardCharsets.UTF_8);
    private static final String BUNDLE_NAME = "com.github.anba.es6draft.runtime.internal.messages";
    private final ResourceBundle resourceBundle;

    private Messages(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    /**
     * Creates a new instance of this class
     */
    public static Messages create(Locale locale) {
        ResourceBundle.Control control = UTF8_RESOURCE_CONTROL;
        ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, locale, control);
        return new Messages(resourceBundle);
    }

    /**
     * Returns the localised message for {@code key} from the resource bundle
     */
    public String getMessage(Key key) {
        try {
            return resourceBundle.getString(key.id);
        } catch (MissingResourceException e) {
            return '!' + key.id + '!';
        }
    }

    /**
     * Returns the localised message for {@code key} from the resource bundle
     */
    public String getMessage(Key key, String... args) {
        try {
            return format(resourceBundle.getString(key.id), resourceBundle.getLocale(), args);
        } catch (MissingResourceException e) {
            return '!' + key.id + '!';
        }
    }

    private String format(String pattern, Locale locale, String... messageArguments) {
        return new MessageFormat(pattern, locale).format(messageArguments);
    }

    /**
     * Message key enumeration
     */
    public enum Key {/* @formatter:off */
        // internal
        InternalError("internal.error"),
        StackOverflow("internal.stackoverflow"),

        // TokenStream
        InvalidNumberLiteral("parser.invalid_number_literal"),
        InvalidBinaryIntegerLiteral("parser.invalid_binary_integer_literal"),
        InvalidOctalIntegerLiteral("parser.invalid_octal_integer_literal"),
        InvalidHexIntegerLiteral("parser.invalid_hex_integer_literal"),
        InvalidNULLEscape("parser.invalid_null_escape"),
        InvalidHexEscape("parser.invalid_hex_escape"),
        InvalidUnicodeEscape("parser.invalid_unicode_escape"),
        UnterminatedStringLiteral("parser.unterminated_string_literal"),
        UnterminatedComment("parser.unterminated_comment"),
        UnterminatedTemplateLiteral("parser.unterminated_template_literal"),
        UnterminatedRegExpLiteral("parser.unterminated_regexp_literal"),
        InvalidRegExpLiteral("parser.invalid_regexp_literal"),
        InvalidUnicodeEscapedIdentifierPart("parser.invalid_unicode_escaped_identifierpart"),
        UnexpectedCharacter("parser.unexpected_character"),
        InvalidToken("parser.invalid_token"),
        UnexpectedToken("parser.unexpected_token"),
        UnexpectedName("parser.unexpected_name"),
        UnicodeEscapeInRegExpFlags("parser.unicode_escape_in_regexp_flags"),

        // Parser
        InvalidFormalParameterList("parser.invalid_formal_parameter_list"),
        InvalidFunctionBody("parser.invalid_function_body"),
        FormalParameterRedeclaration("parser.formal_parameter_redeclaration"),
        UnexpectedEndOfLine("parser.unexpected_end_of_line"),
        MissingSemicolon("parser.missing_semicolon"),
        EmptyParenthesisedExpression("parser.empty_parenthesised_expression"),
        InvalidSpreadExpression("parser.invalid_spread_expression"),
        InvalidConstructorMethod("parser.invalid_constructor_method"),
        InvalidPrototypeMethod("parser.invalid_prototype_method"),
        InvalidSuperExpression("parser.invalid_super_expression"),
        SuperOutsideClass("parser.super_outside_class"),
        MissingColonAfterPropertyId("parser.missing_colon_after_property_id"),
        DuplicatePropertyDefinition("parser.duplicate_property_definition"),
        InvalidReturnStatement("parser.invalid_return_statement"),
        InvalidYieldExpression("parser.invalid_yield_expression"),
        DuplicateLabel("parser.duplicate_label"),
        LabelTargetNotFound("parser.label_target_not_found"),
        InvalidBreakTarget("parser.invalid_break_target"),
        InvalidContinueTarget("parser.invalid_continue_target"),
        InvalidIncDecTarget("parser.invalid_incdec_target"),
        InvalidAssignmentTarget("parser.invalid_assignment_target"),
        InvalidForInOfHead("parser.invalid_for_inof_head"),
        InvalidDestructuring("parser.invalid_destructuring"),
        DestructuringMissingInitialiser("parser.destructuring_missing_initialiser"),
        ConstMissingInitialiser("parser.const_missing_initialiser"),

        // strict mode TokenStream/Parser errors
        StrictModeDuplicateFormalParameter("parser.strict.duplicate_formal_parameter"),
        StrictModeRestrictedIdentifier("parser.strict.restricted_identifier"),
        StrictModeWithStatement("parser.strict.with_statement"),
        StrictModeInvalidAssignmentTarget("parser.strict.invalid_assignment_target"),
        StrictModeInvalidDeleteOperand("parser.strict.invalid_delete_operand"),
        StrictModeInvalidIdentifier("parser.strict.invalid_identifier"),
        StrictModeOctalIntegerLiteral("parser.strict.octal_integer_literal"),
        StrictModeOctalEscapeSequence("parser.strict.octal_escape_sequence"),

        // JSONParser, JSONTokenStream
        JSONUnterminatedStringLiteral("json.unterminated_string_literal"),
        JSONInvalidStringLiteral("json.invalid_string_literal"),
        JSONInvalidUnicodeEscape("json.invalid_unicode_escape"),
        JSONInvalidNumberLiteral("json.invalid_number_literal"),

        // RegExpParser
        RegExpInvalidQuantifier("regexp.invalid_quantifier"),
        RegExpInvalidCharacterRange("regexp.invalid_character_range"),
        RegExpTrailingSlash("regexp.trailing_slash"),
        RegExpUnmatchedCharacter("regexp.unmatched_character"),
        RegExpPatternTooComplex("regexp.pattern_too_complex"),
        RegExpUnexpectedCharacter("regexp.unexpected_character"),

        // SyntaxError
        VariableRedeclaration("syntax.variable_redeclaration"),
        InvalidDeclaration("syntax.invalid_declaration"),
        UnqualifiedDelete("syntax.unqualified_delete"),
        // ReferenceError
        MissingSuperBinding("reference.missing_super_binding"),
        SuperDelete("reference.super_delete"),
        UninitialisedBinding("reference.uninitialised_binding"),
        UnresolvableReference("reference.unresolvable_reference"),
        InvalidReference("reference.invalid_reference"),
        // TypeError
        ImmutableBinding("type.immutable_binding"),
        PropertyNotModifiable("type.property_not_modifiable"),
        PropertyNotCreatable("type.property_not_creatable"),
        PropertyNotDeletable("type.property_not_deletable"),
        StrictModePoisonPill("type.strict_mode_poison_pill"),
        UndefinedOrNull("type.undefined_or_null"),
        MethodNotFound("type.method_not_found"),
        NotCallable("type.not_callable"),
        NotConstructor("type.not_constructor"),
        NotPrimitiveType("type.not_primitive_type"),
        NotObjectType("type.not_object_type"),
        NotObjectOrNull("type.not_object_or_null"),
        NotUndefined("type.not_undefined"),
        NotSymbol("type.not_symbol"),
        NotExtensible("type.not_extensible"),
        IncompatibleObject("type.incompatible_object"),
        InitialisedObject("type.initialised_object"),
        UninitialisedObject("type.uninitialised_object"),
        SymbolObject("type.symbol_object"),
        SymbolPrimitive("type.symbol_primitive"),
        SymbolString("type.symbol_string"),
        SymbolCreate("type.symbol_create"),
        CyclicProto("type.cyclic_proto"),
        PropertyNotFound("type.property_not_found"),

        // 6.2.5 The Property Descriptor Specification Type
        InvalidGetter("propertydescriptor.invalid_getter"),
        InvalidSetter("propertydescriptor.invalid_setter"),
        InvalidDescriptor("propertydescriptor.invalid_descriptor"),
        // ToIndex
        InvalidIndex("abstractops.invalid_index"),
        // 7.1.1 ToPrimitive
        InvalidToPrimitiveHint("abstractops.invalid_to_primitive_hint"),
        NoPrimitiveRepresentation("abstractops.no_primitive_representation"),

        // 18 The Global Object
        MalformedURI("globalobject.malformed_uri"),
        // 19.1 Object Objects
        ObjectSealFailed("object.seal_failed"),
        ObjectFreezeFailed("object.freeze_failed"),
        ObjectPreventExtensionsFailed("object.preventextension_failed"),
        ObjectSetProtoCrossRealm("object.set_proto_cross_realm"),
        // 19.2 Function Objects
        FunctionTooManyArguments("function.too_many_arguments"),
        GeneratorExecuting("generator.executing"),
        GeneratorClosed("generator.closed"),
        GeneratorNewbornSend("generator.newborn_send"),
        // 22.1 Array Objects
        InvalidArrayLength("array.invalid_array_length"),
        ReduceInitialValue("array.reduce_initial_value"),
        // 21.1 String Objects
        InvalidStringRepeat("string.invalid_string_repeat"),
        InvalidCodePoint("string.invalid_codepoint"),
        InvalidNormalizationForm("string.invalid_normalization_form"),
        InvalidRegExpArgument("string.invalid_regexp_argument"),
        // 20.1 Number Objects
        InvalidRadix("number.invalid_radix"),
        InvalidPrecision("number.invalid_precision"),
        // 20.3 Date Objects
        InvalidDateValue("date.invalid_datevalue"),
        // 21.2 RegExp Objects
        DuplicateRegExpFlag("regexp.duplicate_flag"),
        InvalidRegExpFlag("regexp.invalid_flag"),
        InvalidRegExpPattern("regexp.invalid_pattern"),
        RegExpAlreadyInitialised("regexp.already_initialised"),
        RegExpNotInitialised("regexp.not_initialised"),
        RegExpHasRestricted("regexp.has_restricted"),
        // 24.3 The JSON Object
        InvalidJSONLiteral("json.invalid_json_literal"),
        CyclicValue("json.cyclic_value"),
        // 24.1 Binary Data Objects
        OutOfMemory("binary.out_of_memory"),
        OutOfMemoryVM("binary.out_of_memory_vm"),
        ArrayOffsetOutOfRange("binary.array_offset_out_of_range"),
        InvalidByteOffset("binary.invalid_byteoffset"),
        InvalidBufferSize("binary.invalid_buffersize"),
        // 23.1 Map Objects
        MapInvalidComparator("map.invalid_comparator"),
        // 23.2 Set Objects
        SetInvalidComparator("set.invalid_comparator"),
        // 26.2 Proxy Objects
        ProxyRevoked("proxy.revoked"),
        ProxyNew("proxy.new"),
        ProxySamePrototype("proxy.same_prototype"),
        ProxyNotObject("proxy.not_object"),
        ProxyNotObjectOrUndefined("proxy.not_object_or_undefined"),
        ProxyIncompatibleDescriptor("proxy.incompatible_descriptor"),
        ProxyDeleteNonConfigurable("proxy.delete_non_configurable"),
        ProxySameValue("proxy.same_value"),
        ProxyNoSetter("proxy.no_setter"),
        ProxyNoGetter("proxy.no_getter"),
        ProxyExtensible("proxy.extensible"),
        ProxyNotExtensible("proxy.not_extensible"),
        ProxyNotConfigurable("proxy.not_configurable"),
        ProxyAbsentOrConfigurable("proxy.absent_or_configurable"),
        ProxyAbsentNotExtensible("proxy.absent_not_extensible"),

        // Promise Object
        PromiseSelfResolution("promise.self_resolution"),
        // Modules
        ModulesUnresolvedModule("modules.unresolved_module"),
        ModulesUnresolvedImport("modules.unresolved_import"),
        ModulesUnresolvedExport("modules.unresolved_export"),
        ModulesDuplicateExport("modules.duplicate_export"),
        ModulesCyclicExport("cyclic_export"),

        // Intl
        IntlStructurallyInvalidLanguageTag("intl.structurally_invalid_language_tag"),
        IntlInvalidOption("intl.invalid_option"),
        IntlInvalidCurrency("intl.invalid_currency"),
        ;
        /* @formatter:on */

        private final String id;

        private Key(String id) {
            this.id = id;
        }
    }
}
