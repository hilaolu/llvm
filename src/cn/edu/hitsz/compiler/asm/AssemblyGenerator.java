package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.ir.InstructionKind;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    private ArrayList<String> asmcode = new ArrayList();

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        for (var instr : originInstructions) {
            if (instr.getKind().equals(InstructionKind.MOV)) {
                if (instr.getFrom() instanceof IRImmediate imm) {
                    var asm_instr = load_imm(Integer.parseInt(instr.getResult().getName()), imm.getValue());
                    asm_instr.forEach(asmcode::add);
                } else if (instr.getFrom() instanceof IRVariable val) {
                    var asm_instr = mv(
                            Integer.parseInt(instr.getResult().getName()),
                            Integer.parseInt(val.getName()));
                    asm_instr.forEach(asmcode::add);
                }

            } else if (instr.getKind().equals(InstructionKind.RET)) {
                if (instr.getReturnValue() instanceof IRVariable val) {
                    var asm_instr = ret(Integer.parseInt(val.getName()));
                    asm_instr.forEach(asmcode::add);
                } else {
                    throw new RuntimeException("");
                }

            } else {
                if (instr.getLHS() instanceof IRVariable lhs &&
                        instr.getRHS() instanceof IRVariable rhs) {
                    String op = "";
                    if (instr.getKind().equals(InstructionKind.MUL)) {
                        op = "mul";
                    } else if (instr.getKind().equals(InstructionKind.SUB)) {
                        op = "sub";
                    } else if (instr.getKind().equals(InstructionKind.ADD)) {
                        op = "add";
                    } else {
                        throw new RuntimeException("");
                    }

                    var asm_instr = arith(Integer.parseInt(instr.getResult().getName()),
                            Integer.parseInt(lhs.getName()),
                            Integer.parseInt(rhs.getName()),
                            op);
                    asm_instr.forEach(asmcode::add);

                } else {
                    throw new RuntimeException("");
                }

            }

        }

    }

    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
        // throw new NotImplementedException();
    }

    private int getVregMemAddr(int vregid) {
        return vregid * 4;
    }

    private String pop(int vregid, int pregid) {
        return "lw " + "x" + pregid + "," + getVregMemAddr(vregid) + "(x0)";
    }

    private String push(int vregid, int pregid) {
        return "sw " + "x" + pregid + "," + getVregMemAddr(vregid) + "(x0)";
    }

    private List<String> load_imm(int vregid, int imm) {
        var result = new ArrayList();
        var pregid = 10;
        result.add("li x" + pregid + "," + imm);
        result.add(push(vregid, pregid));
        return result;
    }

    private List<String> mv(int ddst, int ssrc) {
        var result = new ArrayList();
        var pregid = 11;
        result.add(pop(ssrc, pregid));
        result.add(push(ddst, pregid));
        return result;
    }

    private List<String> arith(int ddst, int ssrc1, int ssrc2, String op) {
        var result = new ArrayList();
        var pregid1 = 10;
        var pregid2 = 11;
        result.add(pop(ssrc1, pregid1));
        result.add(pop(ssrc2, pregid2));
        var arith_instr = op + " x" + pregid1 + ",x" + pregid1 + ",x" + pregid2;
        result.add(arith_instr);
        result.add(push(ddst, pregid1));
        return result;
    }

    private List<String> ret(int ssrc) {
        var result = new ArrayList();
        var pregid = 10; // a0
        result.add(pop(ssrc, pregid));
        return result;
    }

    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {

        FileUtils.writeLines(path, asmcode);
    }
}
