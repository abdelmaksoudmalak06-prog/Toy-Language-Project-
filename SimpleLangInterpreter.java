import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

public class SimpleLangInterpreter extends AbstractParseTreeVisitor<Integer> implements SimpleLangVisitor<Integer> {

    private final Map<String, SimpleLangParser.DecContext> global_funcs = new HashMap<>();
    private final Stack<Map<String, Integer>> frames = new Stack<>();

    public Integer visitProgram(SimpleLangParser.ProgContext ctx, String[] args) {

        for (int i = 0; i < ctx.dec().size(); ++i) {
            SimpleLangParser.DecContext dec = ctx.dec(i);
            SimpleLangParser.Typed_idfrContext typedIdfr = dec.typed_idfr(0);
            global_funcs.put(typedIdfr.Idfr().getText(), dec);
        }

        SimpleLangParser.DecContext main = global_funcs.get("main");
        Map<String, Integer> newFrame = new HashMap<>();


        for (int i = 0; i < args.length; ++i) {
            String paramName;
            if (main.vardec != null && main.vardec.size() > i) {
                paramName = main.vardec.get(i).Idfr().getText();
            } else {
                // fallback in case vardec is empty but typed_idfr has them
                paramName = main.typed_idfr(i + 1).Idfr().getText();
            }

            if (args[i].equals("true")) {
                newFrame.put(paramName, 1);
            } else if (args[i].equals("false")) {
                newFrame.put(paramName, 0);
            } else {
                newFrame.put(paramName, Integer.parseInt(args[i]));
            }
        }

        frames.push(newFrame);
        return visit(main);
    }


    //NON terminals
    public Integer visitProg(SimpleLangParser.ProgContext ctx) {
        Integer result = 0;

        for (SimpleLangParser.DecContext d : ctx.dec()) {
            result = visit(d);
        }

        return result;
    }



    @Override
    public Integer visitDec(SimpleLangParser.DecContext ctx) {
        return visit(ctx.body());
    }



    @Override
    public Integer visitTyped_idfr(SimpleLangParser.Typed_idfrContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Integer visitType(SimpleLangParser.TypeContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Integer visitBody(SimpleLangParser.BodyContext ctx) {

        Map<String, Integer> frame = frames.peek();

        int numDecls = ctx.typed_idfr().size();

        // Initialise typed local variables
        for (int i = 0; i < numDecls; i++) {
            String name = ctx.typed_idfr(i).Idfr().getText();
            Integer value = visit(ctx.exp(i));
            frame.put(name, value);
        }

        // Evaluate expressions IN ENE (already in correct order)
        Integer result = null;

        for (SimpleLangParser.ExpContext e : ctx.ene().exp()) {
            result = visit(e);
        }

        return result; // return the last expression value
    }



    @Override
    public Integer visitBlock(SimpleLangParser.BlockContext ctx) {
        Integer result = null;

        for (SimpleLangParser.ExpContext e : ctx.ene().exp()) {
            result = visit(e);
        }

        return result;
    }

    // expressions
    @Override
    public Integer visitIdExpr(SimpleLangParser.IdExprContext ctx) {
        return frames.peek().get(ctx.Idfr().getText());
    }


    @Override
    public Integer visitBoollitExpr(SimpleLangParser.BoollitExprContext ctx) {
        if (ctx.boollit() instanceof SimpleLangParser.TrueLiteralContext) {
            return 1;
        } else {
            return 0;
        }

    }

    @Override
    public Integer visitIntExpr(SimpleLangParser.IntExprContext ctx) {

        return Integer.parseInt(ctx.IntLit().getText());

    }

    @Override
    public Integer visitAssignExpr(SimpleLangParser.AssignExprContext ctx) {

        String name = ctx.Idfr().getText();
        Integer value = visit(ctx.exp());

        Map<String, Integer> frame = frames.peek();

        if (!frame.containsKey(name)) {
            throw new RuntimeException("Error: variable '" + name + "' not declared in this scope.");
        }

        frame.put(name, value);   // update value

        return value;             // assignment returns the assigned value
    }

    @Override
    public Integer visitBinOpExpr(SimpleLangParser.BinOpExprContext ctx) {

        SimpleLangParser.ExpContext operand1 = ctx.exp(0);
        Integer oprnd1 = visit(operand1);
        SimpleLangParser.ExpContext operand2 = ctx.exp(1);
        Integer oprnd2 = visit(operand2);

        switch (((TerminalNode) (ctx.binop().getChild(0))).getSymbol().getType()) {

            case SimpleLangParser.Eq -> {

                return ((Objects.equals(oprnd1, oprnd2)) ? 1 : 0);

            }
            case SimpleLangParser.Less -> {

                return ((oprnd1 < oprnd2) ? 1 : 0);

            }
            case SimpleLangParser.LessEq -> {

                return ((oprnd1 <= oprnd2) ? 1 : 0);

            }
            case SimpleLangParser.Great -> {
                return ((oprnd1 > oprnd2) ? 1 : 0);
            }
            case SimpleLangParser.GreatEq -> {

                return ((oprnd1 >= oprnd2) ? 1 : 0);
            }

            case SimpleLangParser.Plus -> {

                return oprnd1 + oprnd2;

            }
            case SimpleLangParser.Minus -> {

                return oprnd1 - oprnd2;

            }
            case SimpleLangParser.Star -> {

                return oprnd1 * oprnd2;

            }
            case SimpleLangParser.Slash -> {
                return oprnd1 / oprnd2;
            }
            case SimpleLangParser.Amp -> {
                return (oprnd1 != 0 && oprnd2 != 0) ? 1 : 0;
            }
            case SimpleLangParser.Caret -> {
                return (oprnd1 ^ oprnd2); // this is XOR baiscally
            }
            case SimpleLangParser.Bar -> {
                return (oprnd1 != 0 || oprnd2 != 0) ? 1 : 0; //OR
            }
            default -> {
                throw new RuntimeException("Shouldn't be here - wrong binary operator.");
            }

        }

    } //done

    @Override
    public Integer visitInvokeExpr(SimpleLangParser.InvokeExprContext ctx) {

        String fname = ctx.Idfr().getText();
        SimpleLangParser.DecContext func = global_funcs.get(fname);

        Map<String, Integer> newFrame = new HashMap<>();

        // parameters start at index 1 — index 0 is the function name
        for (int i = 0; i < ctx.args().exp().size(); i++) {
            int argValue = visit(ctx.args().exp(i));
            String paramName = func.typed_idfr(i + 1).Idfr().getText();
            newFrame.put(paramName, argValue);
        }

        frames.push(newFrame);

        Integer result = visit(func.body());

        frames.pop();
        return result;
    }




    @Override
    public Integer visitBlockExpr(SimpleLangParser.BlockExprContext ctx) {
        return visit(ctx.block());
    }


    @Override
    public Integer visitUnopExpr(SimpleLangParser.UnopExprContext ctx) {

        Integer value = visit(ctx.exp());
        String op = ctx.unop().getText();

        if (op.equals("~")) {
            // boolean NOT
            return (value == 0 ? 1 : 0);
        }

        if (op.equals("-")) {
            // arithmetic negation
            return -value;
        }

        throw new RuntimeException("Unknown unary operator: " + op);
    }


    @Override
    public Integer visitIfExpr(SimpleLangParser.IfExprContext ctx) {

        SimpleLangParser.ExpContext cond = ctx.exp();
        Integer condValue = visit(cond);
        if (condValue != 0) {

            SimpleLangParser.BlockContext thenBlock = ctx.block(0);
            return visit(thenBlock);

        } else {

            SimpleLangParser.BlockContext elseBlock = ctx.block(1);
            return visit(elseBlock);

        }

    }

    @Override
    public Integer visitWhileExpr(SimpleLangParser.WhileExprContext ctx) {

        SimpleLangParser.ExpContext cond = ctx.exp();
        Integer condValue = visit(cond);

        Integer lastValue = null;

        while (condValue != 0) {
            lastValue = visit(ctx.block());  // save last block value
            condValue = visit(cond);         // re-evaluate condition
        }

        return lastValue;                    // return last value (or null if  we never ran)
    }//done


    @Override
    public Integer visitRepeatExpr(SimpleLangParser.RepeatExprContext ctx) {

        Integer lastValue = visit(ctx.block());    // ALWAYS execute one time first based on semantics

        Integer condValue = visit(ctx.exp());      // Evaluate condition AFTER first iteration

        while (condValue == 0) {                   // Continue WHILE false (0)
            lastValue = visit(ctx.block());        // Execute block again
            condValue = visit(ctx.exp());          // Re-evaluate condition
        }

        return lastValue;                          // Return last block value
    }//done


    @Override
    public Integer visitPrintExpr(SimpleLangParser.PrintExprContext ctx) {

        SimpleLangParser.ExpContext exp = ctx.exp();

        int tokenType = ((TerminalNode) exp.getChild(0)).getSymbol().getType();

        if (tokenType == SimpleLangParser.Space) {

            // space prints a single space
            System.out.print(" ");
            return null;

        } else if (tokenType == SimpleLangParser.Newline) {

            // newline prints a line break
            System.out.println();
            return null;

        } else if (tokenType == SimpleLangParser.Skip) {

            // skip does nothing
            return null;

        } else {

            // for all other expressions, print their evaluated value
            System.out.print(visit(exp));
            return null;
        }
    }


    @Override
    public Integer visitSpaceExpr(SimpleLangParser.SpaceExprContext ctx) {
        System.out.print(" ");
        return null;
    }

    @Override
    public Integer visitNewlineExpr(SimpleLangParser.NewlineExprContext ctx) {
        System.out.println();
        return null;
    }


    @Override
    public Integer visitSkipExpr(SimpleLangParser.SkipExprContext ctx) {
        // skip produces no output
        return null;
    }

    @Override
    public Integer visitArgs(SimpleLangParser.ArgsContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Integer visitEqBinop(SimpleLangParser.EqBinopContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Integer visitLessBinop(SimpleLangParser.LessBinopContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Integer visitGreaterBinop(SimpleLangParser.GreaterBinopContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Integer visitLessEqBinop(SimpleLangParser.LessEqBinopContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Integer visitGreaterEqBinop(SimpleLangParser.GreaterEqBinopContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Integer visitStarBinop(SimpleLangParser.StarBinopContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Integer visitSlashBinop(SimpleLangParser.SlashBinopContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Integer visitPlusBinop(SimpleLangParser.PlusBinopContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Integer visitMinusBinop(SimpleLangParser.MinusBinopContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Integer visitAmpBinop(SimpleLangParser.AmpBinopContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Integer visitBarBinop(SimpleLangParser.BarBinopContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Integer visitCaretBinop(SimpleLangParser.CaretBinopContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Integer visitTildeUnop(SimpleLangParser.TildeUnopContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Integer visitMinusUnop(SimpleLangParser.MinusUnopContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Integer visitTrueLiteral(SimpleLangParser.TrueLiteralContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Integer visitFalseLiteral(SimpleLangParser.FalseLiteralContext ctx) {
        return visitChildren(ctx);
    }
    @Override
    public Integer visitEne (SimpleLangParser.EneContext ctx) {
        return visitChildren(ctx);
    }


}

