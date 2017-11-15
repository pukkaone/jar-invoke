grammar JarInvoke;

@header {
package com.github.pukkaone.jarinvoke.parser;
}

ASSIGN: '=';
COMMA: ',';
DOT: '.';
PAREN_OPEN: '(';
PAREN_CLOSE: ')';

STRING_LITERAL
  : '\'' ~('\'')*? '\''
  | '"' ~('"')*? '"'
  ;

WHITESPACE
  : [\f\n\r\t ]+ -> skip
  ;

INVOKE: 'invoke';
LOAD: 'load';

IDENTIFIER
	:	Letter LetterOrDigit*
	;

fragment Letter
	:	[A-Za-z$_]
	;

fragment LetterOrDigit
	:	[0-9A-Za-z$_]
	;

translationUnit
  : statement* EOF
  ;

statement
  : invokeExpression
  | loadStatement
  | requireStatement
  ;

invokeExpression
  : IDENTIFIER '.' 'invoke' '(' STRING_LITERAL ',' STRING_LITERAL ')'
  ;

loadStatement
  : IDENTIFIER '=' 'load' '(' STRING_LITERAL ',' STRING_LITERAL ')'
  ;

requireStatement
  : IDENTIFIER '=' 'require' '(' STRING_LITERAL ',' STRING_LITERAL ')'
  ;
