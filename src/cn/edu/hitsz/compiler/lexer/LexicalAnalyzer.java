package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
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

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
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

            expr.forEach(token_buf::add);

        } else {

            var words = sentence.split(" ");

            words[0] = words[0].trim();
            words[1] = words[1].trim();

            if (words[0].equals("int")) {

                assert (words.length == 2);

                token_buf.add(Token.simple("int"));
                token_buf.add(Token.normal("id", words[1]));

            } else if (words[0].equals("return")) {
                assert (words.length == 2);
                token_buf.add(Token.simple("return"));

                var expr = parse_expr(words[1]);
                expr.forEach(token_buf::add);

            }

        }

        return token_buf;

    }

    private Iterable<Token> parse_expr(String expr) {
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
