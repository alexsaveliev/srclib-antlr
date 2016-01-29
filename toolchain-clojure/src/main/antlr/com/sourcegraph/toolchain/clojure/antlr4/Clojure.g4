/* Reworked for grammar specificity by Reid Mckenzie. Did a bunch of
   work so that rather than reading "a bunch of crap in parens" some
   syntactic information is preserved and recovered. Dec. 14 2014.

   Converted to ANTLR 4 by Terence Parr. Unsure of provence. I see
   it commited by matthias.koester for clojure-eclipse project on
   Oct 5, 2009:

   https://code.google.com/p/clojure-eclipse/

   Seems to me Laurent Petit had a version of this. I also see
   Jingguo Yao submitting a link to a now-dead github project on
   Jan 1, 2011.

   https://github.com/laurentpetit/ccw/tree/master/clojure-antlr-grammar

   Regardless, there are some issues perhaps related to "sugar";
   I've tried to fix them.

   This parses https://github.com/weavejester/compojure project.

   I also note this is hardly a grammar; more like "match a bunch of
   crap in parens" but I guess that is LISP for you ;)
 */

/*
* Based on https://raw.githubusercontent.com/antlr/grammars-v4/fd8dc428039208dc25c0803f5d6ff38e6750d4e4/swift/Swift.g4
*/
grammar Clojure;

@header {
    package com.sourcegraph.toolchain.clojure.antlr4;
}

file: form *;

form: function_def
    | var_def
    | in_ns_def
    | ns_def
    | let_form
    | literal
    | list
    | vector
    | map
    | reader_macro
    ;

/* function definitions */
function_def: '(' fn_start metadata_form? fn_name docstring? '[' arguments last_arguments? ']' forms ')';

fn_start: 'defn'
        | 'defn-'
        | 'defmacro';

fn_name: symbol;

metadata_form: meta_tag;

meta_tag: '^' form;

docstring: string;

arguments: parameter*;

parameter: meta_tag? parameter_name;

parameter_name: symbol;

last_arguments: '&' arguments;

/* variable definitions */

var_def: '(' var_start var_name forms ')';

var_start: 'def'
         | 'defonce';

var_name: symbol;

/* in-ns namespace def */

in_ns_def: '(' 'in-ns' '\'' ns_name ')';

/* ns simple definition */
ns_def: '(' 'ns' ns_name docstring? attr_map? references ')';

ns_name: symbol;

attr_map: map;

references: reference*;

reference: require_reference
         | use_reference
         | import_reference
         | other_reference
         ;

require_reference: '(' ':require' ref_entities ')';

use_reference: '(' ':use' ref_entities ')';

import_reference: '(' ':import' ref_entities ')';

other_reference: '(' keyword forms ')'; // unsupported for now cases

ref_entities: ref_entity*;

ref_entity: ref_keyword // unsupported for now cases
          | list // unsupported for now cases
          | symbol
          | ref_vector // unsupported for now cases
          ;

ref_keyword: keyword;
ref_vector: vector;

/* let_form */
let_form: '(' 'let' '[' bindings ']' forms ')';

bindings: binding* ;

//binding: var_name form #var_form_binding //supported case for now
//       | form #formbinding; //all others, unsupported

binding: var_name form //supported case for now
       | form;  //all others, unsupported

forms: form* ;

list:  '(' forms ')' ;

vector: '[' forms ']' ;

map: '{' (form form)* '}' ;

set: '#{' forms '}' ;

reader_macro
    : lambda
    | meta_data
    | regex
    | var_quote
    | host_expr
    | set
    | tag
    | discard
    | dispatch
    | deref
    | quote
    | backtick
    | unquote
    | unquote_splicing
    | gensym
    ;

// TJP added '&' (gather a variable number of arguments)
quote
    : '\'' form
    ;

backtick
    : '`' form
    ;

unquote
    : '~' form
    ;

unquote_splicing
    : '~@' form
    ;

tag
    : '^' form form
    ;

deref
    : '@' form
    ;

gensym
    : SYMBOL '#'
    ;

lambda
    : '#(' form* ')'
    ;

meta_data
    : '#^' (map form | form)
    ;

var_quote
    : '#\'' symbol
    ;

host_expr
    : '#+' form form
    ;

discard
    : '#_' form
    ;

dispatch
    : '#' symbol form
    ;

regex
    : '#' string
    ;

literal
    : string
    | number
    | character
    | nil
    | BOOLEAN
    | keyword
    | symbol
    | param_name
    ;

string: STRING;
hex: HEX;
bin: BIN;
bign: BIGN;
number
    : FLOAT
    | hex
    | bin
    | bign
    | LONG
    ;

character
    : named_char
    | u_hex_quad
    | any_char
    ;
named_char: CHAR_NAMED ;
any_char: CHAR_ANY ;
u_hex_quad: CHAR_U ;

nil: NIL;

keyword: macro_keyword | simple_keyword;
simple_keyword: ':' symbol;
macro_keyword: ':' ':' symbol;

symbol: ns_symbol | simple_sym;
simple_sym: SYMBOL;
ns_symbol: NS_SYMBOL;

param_name: PARAM_NAME;

// Lexers
//--------------------------------------------------------------------

STRING : '"' ( ~'"' | '\\' '"' )* '"' ;

// FIXME: Doesn't deal with arbitrary read radixes, BigNums
FLOAT
    : '-'? [0-9]+ FLOAT_TAIL
    | '-'? 'Infinity'
    | '-'? 'NaN'
    ;

fragment
FLOAT_TAIL
    : FLOAT_DECIMAL FLOAT_EXP
    | FLOAT_DECIMAL
    | FLOAT_EXP
    ;

fragment
FLOAT_DECIMAL
    : '.' [0-9]+
    ;

fragment
FLOAT_EXP
    : [eE] '-'? [0-9]+
    ;
fragment
HEXD: [0-9a-fA-F] ;
HEX: '0' [xX] HEXD+ ;
BIN: '0' [bB] [10]+ ;
LONG: '-'? [0-9]+[lL]?;
BIGN: '-'? [0-9]+[nN];

CHAR_U
    : '\\' 'u'[0-9D-Fd-f] HEXD HEXD HEXD ;
CHAR_NAMED
    : '\\' ( 'newline'
           | 'return'
           | 'space'
           | 'tab'
           | 'formfeed'
           | 'backspace' ) ;
CHAR_ANY
    : '\\' . ;

NIL : 'nil';

BOOLEAN : 'true' | 'false' ;

SYMBOL
    : '.'
    | '/'
    | NAME
    ;

NS_SYMBOL
    : NAME '/' SYMBOL
    ;

PARAM_NAME: '%' ((('1'..'9')('0'..'9')*)|'&')? ;

// Fragments
//--------------------------------------------------------------------

fragment
NAME: SYMBOL_HEAD SYMBOL_REST* (':' SYMBOL_REST+)* ;

fragment
SYMBOL_HEAD
    : ~('0' .. '9'
        | '^' | '`' | '\'' | '"' | '#' | '~' | '@' | ':' | '/' | '%' | '(' | ')' | '[' | ']' | '{' | '}' // FIXME: could be one group
        | [ \n\r\t\,] // FIXME: could be WS
        )
    ;

fragment
SYMBOL_REST
    : SYMBOL_HEAD
    | '0'..'9'
    | '.'
    ;

// Discard
//--------------------------------------------------------------------

fragment
WS : [ \n\r\t\,] ;

fragment
COMMENT: ';' ~[\r\n]* ;

TRASH
    : ( WS | COMMENT ) -> channel(HIDDEN)
    ;
