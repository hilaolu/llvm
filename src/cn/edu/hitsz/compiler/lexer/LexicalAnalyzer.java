package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;

    private String code;
    private ArrayList tokens = new ArrayList<>();

    private ArrayList instructions = new ArrayList<>();

    private HashMap<String, Integer> id2vreg = new HashMap<String, Integer>();

    private HashMap<String, Integer> vreg2ir = new HashMap<String, Integer>();

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public List<Instruction> getIR() {
        return instructions;
    }

    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // TODO: 词法分析前的缓冲区实现
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法

        code = FileUtils.readFile(path);

    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // TODO: 自动机实现的词法分析过程

        code = code.replaceAll("[\\n\t]", "");
        // System.out.print(code);
        var sentences = code.split(";", 0);

        for (var sentence : sentences) {

            var parse_result = parse_sentence(sentence);

            parse_result.forEach(tokens::add);

            tokens.add(Token.simple("Semicolon"));
        }

        tokens.add(Token.eof());
    }

    public void printIR() {

        for (var instr : instructions) {
            System.out.println(instr);
        }

    }

    private Iterable<Token> parse_sentence(String sentence) {

        sentence = sentence.trim();

        // we assume the input is legal
        List<Token> token_buf = new ArrayList<Token>();

        if (sentence.contains("=")) {

            var words = sentence.split("=");
            words[0] = words[0].trim();
            words[1] = words[1].trim();

            // we assume the id is legal
            token_buf.add(Token.normal("id", words[0]));
            token_buf.add(Token.simple("="));

            var expr = parse_expr(words[1]);
            var result_vregid = expr2tree(expr);
            assign_val(id2vreg.get(words[0]), result_vregid);

            expr.forEach(token_buf::add);

        } else {

            var words = sentence.split(" ");

            words[0] = words[0].trim();
            words[1] = words[1].trim();

            if (words[0].equals("int")) {

                assert (words.length == 2);

                vreg_id++;
                id2vreg.put(words[1], vreg_id);

                token_buf.add(Token.simple("int"));
                token_buf.add(Token.normal("id", words[1]));

            } else if (words[0].equals("return")) {
                assert (words.length == 2);
                token_buf.add(Token.simple("return"));

                var expr = parse_expr(words[1]);

                var result_vregid = expr2tree(expr);
                ret_val(result_vregid);

                expr.forEach(token_buf::add);

            }

        }

        return token_buf;

    }

    private Iterable<Token> parse_expr(String expr) {
        expr = expr.replaceAll("[ ]", "");
        var symbols = List.of('(', ')', '*', '/', '+');

        var chars = expr.toCharArray();

        var token_buf = new ArrayList<Token>();

        var ptr = 0;

        var last_token_is_symbol = true;

        while (true) {
            if (ptr >= chars.length)
                break;

            if (chars[ptr] == ' ') {
                ptr++;
                continue;
            }

            if (symbols.contains(chars[ptr])) {
                token_buf.add(Token.simple(chars[ptr] + ""));
                last_token_is_symbol = true;
                ptr++;
            } else if (chars[ptr] == '-') {
                if (last_token_is_symbol) {
                    var str_buf = "-";
                    ptr++;

                    while (ptr < chars.length && '0' <= chars[ptr] && chars[ptr] <= '9') {
                        str_buf += chars[ptr];
                        ptr++;
                    }

                    token_buf.add(Token.normal("IntConst", str_buf));

                    last_token_is_symbol = false;
                } else {
                    token_buf.add(Token.simple(chars[ptr] + ""));
                    last_token_is_symbol = true;
                    ptr++;
                }

            } else if ('0' <= chars[ptr] && chars[ptr] <= '9') {

                var str_buf = "";

                while (ptr < chars.length && '0' <= chars[ptr] && chars[ptr] <= '9') {
                    str_buf += chars[ptr];
                    ptr++;
                }

                token_buf.add(Token.normal("IntConst", str_buf));

                last_token_is_symbol = false;
            } else if (true) {
                // we assume it is among [a-zA-Z]

                var str_buf = "";

                while (ptr < chars.length && !symbols.contains(chars[ptr]) && chars[ptr] != '-') {
                    str_buf += chars[ptr];
                    ptr++;
                }

                token_buf.add(Token.normal("id", str_buf));
                last_token_is_symbol = false;

            }
        }

        return token_buf;

    }

    private int vreg_id = 0;

    public int expr2tree(Iterable<Token> tokens) {
        var tokens_list = new ArrayList<Token>();
        tokens.forEach(tokens_list::add);

        // find last +/-

        int last_plus_index = -1;
        int last_multiply_index = -1;

        for (int i = 0; i < tokens_list.size(); i++) {
            if (tokens_list.get(i).getKind().getIdentifier().equals("+") ||
                    tokens_list.get(i).getKind().getIdentifier().equals("-")) {
                last_plus_index = i;
            }

            if (tokens_list.get(i).getKind().getIdentifier().equals("*") ||
                    tokens_list.get(i).getKind().getIdentifier().equals("/")) {
                last_multiply_index = i;

            }

            // skip bracket
            if (tokens_list.get(i).getKind().getIdentifier().equals("(")) {
                var left_bracket_num = 1;
                while (left_bracket_num != 0) {
                    i++;
                    if (tokens_list.get(i).getKind().getIdentifier().equals(")")) {
                        left_bracket_num--;
                    }
                    if (tokens_list.get(i).getKind().getIdentifier().equals("(")) {
                        left_bracket_num++;
                    }
                }
            }

        }

        var last_op_index = -1;
        if (last_plus_index != -1) {
            last_op_index = last_plus_index;
        } else if (last_multiply_index != -1) {
            last_op_index = last_multiply_index;
        }

        if (tokens_list.size() == 1) {
            // the expr is a number/id
            // both id and const can be process this way
            var token = tokens_list.get(0);

            if (token.getKindId().equals("id")) {
                int vreg = id2vreg.get(token.getText());
                return vreg;
            } else {

                // if the token is a intconst, we creae a vreg for it
                vreg_id++;
                assign_imm(vreg_id, Integer.parseInt(tokens_list.get(0).getText()));
                return vreg_id;
            }

        } else if (last_op_index == -1) {
            // the expr is prime, remove the bracket around it
            return expr2tree(tokens_list.subList(1, tokens_list.size() - 1));

        } else {
            var lhs = expr2tree(tokens_list.subList(0, last_op_index));
            var rhs = expr2tree(tokens_list.subList(last_op_index + 1, tokens_list.size()));

            vreg_id++;
            var op = tokens_list.get(last_op_index).getKind().getIdentifier();
            arith(vreg_id, lhs, rhs, op);
            return vreg_id;

        }

    }

    private void arith(int ddst, int ssrc1, int ssrc2, String op) {
        var dst = IRVariable.named("" + ddst);
        var src1 = IRVariable.named("" + ssrc1);
        var src2 = IRVariable.named("" + ssrc2);
        Instruction instr = null;
        switch (op) {
            case "+":
                instr = Instruction.createAdd(dst, src1, src2);
                break;
            case "*":
                instr = Instruction.createMul(dst, src1, src2);
                break;
            case "-":
                instr = Instruction.createSub(dst, src1, src2);
                break;
            default:
                throw new RuntimeException("");

        }

        instructions.add(instr);

    }

    private void assign_imm(int vreg_id, int value) {
        var imm = IRImmediate.of(value);
        var result = IRVariable.named("" + vreg_id);
        var instr = Instruction.createMov(result, imm);
        instructions.add(instr);
    }

    private void assign_val(int ddst, int ssrc) {
        var dest = IRVariable.named("" + ddst);
        var src = IRVariable.named("" + ssrc);
        var instr = Instruction.createMov(dest, src);
        instructions.add(instr);
    }

    private void ret_val(int ssrc) {
        var src = IRVariable.named("" + ssrc);
        var instr = Instruction.createRet(src);
        instructions.add(instr);
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
                path,
                StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList());
    }

}
