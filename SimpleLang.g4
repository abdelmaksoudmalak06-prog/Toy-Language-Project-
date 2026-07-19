grammar SimpleLang;

prog : dec+ EOF;

dec
    : typed_idfr LParen (vardec+=typed_idfr(Comma typed_idfr)*)? RParen body
;

typed_idfr
    : type Idfr
;

type
    : IntType
     | BoolType
     | UnitType
;

body : LBrace (typed_idfr Assign exp Semicolon)* ene RBrace ;
block : LBrace ene RBrace ;
ene   : exp (Semicolon exp)* ;


exp
     : Idfr                                                  #IdExpr
     | boollit                                               #BoollitExpr
    | IntLit                                                #IntExpr
    | Idfr Assign exp                                       #AssignExpr
    | LParen exp binop exp RParen                           #BinOpExpr
    | Idfr LParen args RParen                               #InvokeExpr
    | block                                             #BlockExpr
    | unop exp                                              #UnopExpr
    | If exp Then block Else block                          #IfExpr
    | While exp Do block                                    #WhileExpr
    | Repeat block Until exp                                 #RepeatExpr
    | Print exp                                             #PrintExpr
    | Space                                                 #SpaceExpr
    |Newline                                                #NewlineExpr
    |Skip                                                   #SkipExpr

;
args :  (exp   (Comma exp)*)? ;

binop
    : Eq              #EqBinop
    | Less            #LessBinop
    |Great          #GreaterBinop
    | LessEq          #LessEqBinop
    |GreatEq        #GreaterEqBinop
    |Star              #StarBinop    //times
    |Slash             #SlashBinop   //divison
    | Plus            #PlusBinop
    | Minus           #MinusBinop
    |Amp              #AmpBinop
    |Bar              #BarBinop
    | Caret           #CaretBinop
;

unop
        : Tilde    #TildeUnop
        | Minus    #MinusUnop

;
boollit
:True               #TrueLiteral
|False              #FalseLiteral
;


If : 'if' ;
Then : 'then' ;
Else : 'else' ;
While: 'while' ;
Do :'do' ;
Repeat: 'repeat' ;
Until: 'until' ;
Print: 'print' ;
Space: 'space' ;
Newline: 'newline' ;
Skip: 'skip' ;
IntType : 'int' ;
BoolType : 'bool' ;
UnitType : 'unit' ;
True: 'true';
False: 'false';


IntLit : '0' | ([1-9][0-9]*) ;  // i removed the ('-'?) because based on running test 20 the unop subsitutes that which
                               // allows for the possibility of having negative numbers


Idfr : [a-z][a-zA-Z0-9_]* ;


Semicolon : ';' ;
LParen : '(' ;
RParen : ')' ;
Eq : '==' ;
Less : '<' ;
LessEq : '<=' ;
Great: '>' ;
GreatEq: '>=' ;
Comma : ',' ;
LBrace  : '{' ;
RBrace   : '}' ;
Assign   : ':=' ;
Star: '*';
Slash: '/';
Plus: '+';
Minus: '-';
Amp: '&' ;
Bar: '|' ;
Caret: '^' ;
Tilde: '~' ;

WS : [ \n\r\t]+ -> skip ;