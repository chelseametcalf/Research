package MyCode;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.HashSet;
import java.util.HashMap;

//import MyCode.AliasedLockOrder.Quad;
//import MyCode.AliasedLockOrder.Triple;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXContainerCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.rta.CallSite;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cfg.ExplodedInterproceduralCFG;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.thin.ThinSlicer;
import com.ibm.wala.shrikeCT.InvalidClassFileException; 
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.strings.StringStuff;
import com.ibm.wala.util.warnings.Warnings;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.analysis.typeInference.*;


public class HadoopAnalyzer_v3 {
    
  //  CGNode => HashSet<SSAInstruction>
  static HashMap<Object, HashSet<Object>> callSites = new HashMap<Object, HashSet<Object>>();

  static String class1 = "Ltest";
  static String method1 = "bar";
  static String class2 = "Ltest";
  static String method2 = "zot";
  static String filterClass = "LtestConf";
  static String filterParType = "Ljava/lang/String;";

  //Should read this from the command line
  static String pType;
  static String targetClassNames;
  static String mainClass;
  static String entryClass;
  static String exclusionsFile;
  static CallGraphBuilder builder;
  static IClassHierarchy cha;
  static CallGraph cg;
  static ExplodedInterproceduralCFG icfg;
  static PointerAnalysis pointerAnalysis; 
  static HeapModel heapModel;
  static Integer kDeep;
  static Integer kBranch;
  static Integer nodeCount;
  static int maxNodePerPath = 0;
  static int maxStatementCount = 0;
  static int maxStatementCountAllPaths = 0;
  static Integer statementCount;
  
  // maps instructions to <cgnode,basicblock>
  static HashMap<SSAInstruction, Triple<Integer, CGNode, IExplodedBasicBlock>> instructionContext = new  HashMap<SSAInstruction, Triple<Integer, CGNode, IExplodedBasicBlock>>();
  
  static ArrayList<SSAInstruction> controlStatements = new ArrayList<SSAInstruction>();
  
  static HashMap<SSAInstruction, Statement> seedControls = new HashMap<SSAInstruction, Statement>();
  static HashMap<SSAInstruction, Statement> seedLeftControls = new HashMap<SSAInstruction, Statement>();
  static HashMap<SSAInstruction, Statement> seedRightControls = new HashMap<SSAInstruction, Statement>();

  static  class Triple<T1, T2, T3> {

    Object val1;
    Object val2;
    Object val3;

    public Triple(Object v1, Object v2, Object v3) 
    {
        val1 = v1;
        val2 = v2;
        val3 = v3; 
    }      
  }

  
  
/**
 * Usage: ScopeFileCallGraph -scopeFile file_path [-entryClass class_name |
 * -mainClass class_name]
 * 
 * If given -mainClass, uses main() method of class_name as entrypoint. If
 * given -entryClass, uses all public methods of class_name.
 * 
 * @throws IOException
 * @throws ClassHierarchyException
 * @throws CallGraphBuilderCancelException
 * @throws IllegalArgumentException
 */
  public static void main(String[] args) throws Exception, IOException, ClassHierarchyException, IllegalArgumentException,
          CallGraphBuilderCancelException, InvalidClassFileException {
  long start = System.currentTimeMillis();
  Properties p = CommandLine.parse(args);
  String scopeFile = p.getProperty("scopeFile");
  entryClass = p.getProperty("entryClass");
  mainClass = p.getProperty("mainClass");
  targetClassNames = p.getProperty("targetClassNames");
  exclusionsFile = p.getProperty("exclusionsFile");
  if (p.getProperty("kDeep") == null)
    kDeep = 0;
  else
    kDeep = Integer.parseInt(p.getProperty("kDeep"));
  
  if (p.getProperty("kBranch") == null)
    kBranch = 0;
  else
    kBranch = Integer.parseInt(p.getProperty("kBranch"));
  
   System.out.println("kDeep=" + kDeep + " kBranch=" + kBranch);

  pType = p.getProperty("pointerAnalysis"); 
  if (pType == null)
     pType = "zeroOneConCFA";
  // Format: Each line represents a separate statement specification
  // className;methodName;lineNo

  if (mainClass != null && entryClass != null) {
    throw new IllegalArgumentException("only specify one of mainClass or entryClass");
  }
  // use exclusions to eliminate certain library packages
  
  //System.out.println("kdeep: " + kDeep + "   kbranch: " + kBranch);

  if (targetClassNames == null)
System.out.println("WARNING: Analysis could be more efficient by specifying a semicolon separated list of target classes (excluding mainClass and entryClass) with -targetClassNames option (use / instead of . in class names)"); 

  System.out.println("building call graph...");
  configureAndCreateCallGraph(scopeFile, mainClass, entryClass); 

//  CallGraphBuilder builder = Util.makeNCFABuilder(2, options, cache, cha, scope);
//  CallGraphBuilder builder = Util.makeVanillaNCFABuilder(2, options, cache, cha, scope);


  pointerAnalysis = builder.getPointerAnalysis();
  heapModel = pointerAnalysis.getHeapModel();
  //System.out.println("Exploding the call graph.." + cg.getClass().getName());
   
  // Create the interprocedural control flow graph=======================================================
  icfg = ExplodedInterproceduralCFG.make(cg);
  ArrayList<SSAInstruction> seedInstr = new ArrayList<SSAInstruction>();
  //ArrayList<Statement> seedStatements = new ArrayList<Statement>();
  
  Integer idx = 0;
  //Integer seedInstrCount = 0;    
  for(CGNode node: icfg.getCallGraph()) 
  {
     idx++;
     // find seed instruction
     //SSAInstruction instr = findCallToInstr(node, "newInstance");
     
     if (!isATarget(node)) continue;
     ExplodedControlFlowGraph graph = (ExplodedControlFlowGraph) icfg.getCFG(node);
     
     if (graph == null) continue; 
     IR ir = node.getIR();
     
     if (ir == null) continue;
     SSAInstruction[] insts = ir.getInstructions();
     for(int i=0; i < insts.length; i++) 
     {
         SSAInstruction inst = insts[i];
         //System.out.println(inst);
         instructionContext.put(inst, new Triple(i, node, graph.getBlockForInstruction(i)));
         addCallSites(node, inst);
     }
  }


  SSAInstruction instr = findCallToMethodCall("Lorg/apache/hadoop/io/SequenceFile$Reader", "init","Lorg/apache/hadoop/util/ReflectionUtils","newInstance");
  //SSAInstruction instr = findCallToMethodCall(class1, method1,class2,method2);
    
      if (instr != null)
     {
        System.out.println("Found the seed instruction: " + prettyPrint(instr));
         seedInstr.add(instr);
         //seedInstrCount++;
         //System.out.println("Seed Instruction Count: " + seedInstrCount);
     }
  
  nodeCount = 0;
  for(SSAInstruction st: seedInstr)
  {
    Triple<Integer, CGNode, IExplodedBasicBlock> contextInfo = instructionContext.get(st);
    CGNode stNode = (CGNode)contextInfo.val2;
    if (!isATarget(stNode)) continue;
    // Explore predecessors of the seed instructions
    explorePredecessorsInterProcedurally(0, 0, 0, new HashSet<CGNode>(), stNode, st);
    nodeCount++;
  }
  
  System.out.println("Control Statements Size: " + controlStatements.size());
  statementCount = 0;
  for(SSAInstruction si: controlStatements)
  {
    Triple<Integer, CGNode, IExplodedBasicBlock> contextInfo = instructionContext.get(si);
    CGNode siNode = (CGNode)contextInfo.val2;
    Statement statement = createStatement(siNode, si);
    
    if (statement != null)
    {
      if (!seedControls.containsValue(statement))
      {
        statementCount++;
        seedControls.put(si, statement);
        Statement leftSideStatement = createStatement(siNode, siNode.getDU().getDef(si.getUse(0)));
        Statement rightSideStatement = createStatement(siNode, siNode.getDU().getDef(si.getUse(1)));
        
        if (leftSideStatement != null)
        {
          if (!seedLeftControls.containsValue(leftSideStatement))
          {
            seedLeftControls.put(si, leftSideStatement);
          }
        }
        
        
        if (rightSideStatement != null)
        {
          if (!seedRightControls.containsValue(rightSideStatement))
          {
            seedRightControls.put(si, rightSideStatement);
          }
        }
      }
    }
  }
  

  Integer branchCount = 0;
  Collection<Statement> slice = null;
  Collection<Statement> leftSlice = null;
  Collection<Statement> rightSlice = null;
  for(SSAInstruction s : seedControls.keySet())
  {
      Statement statement = seedControls.get(s);
      branchCount++;
      ThinSlicer ts = new ThinSlicer(cg,pointerAnalysis);
      slice = ts.computeBackwardThinSlice(statement);
      System.out.println();
      System.out.println(prettyPrint(s));
      System.out.println("Branch In Set: " + branchCount + " << ");
      dumpSlice(statement, slice);
      
      if (seedLeftControls.containsKey(s))
      {
        Statement statementLeft = seedLeftControls.get(s);
        //ThinSlicer tsLeft = new ThinSlicer(cg,pointerAnalysis);
        leftSlice = ts.computeBackwardThinSlice(statementLeft);
        System.out.println("Operand 1's Slice: ** ");
        dumpSlice(statementLeft, leftSlice);
        System.out.println(" **");
      }
      
      
      if (seedRightControls.containsKey(s))
      {
        Statement statementRight = seedRightControls.get(s);
        System.out.println("Right Statement: " + statementRight);
        //ThinSlicer tsRight = new ThinSlicer(cg,pointerAnalysis);
        rightSlice = ts.computeBackwardThinSlice(statementRight);
        System.out.println("Operand 2's Slice: ** ");
        dumpSlice(statementRight, rightSlice);
        System.out.println(" **");
      }
      System.out.println(" >>");
  }
  
  System.out.println("The max node count is: " + maxNodePerPath);
  System.out.println("The statement count is: " + statementCount);
  
  long end = System.currentTimeMillis();
  System.out.println("done");
  System.out.println("took " + (end-start) + "ms");
  System.out.println(CallGraphStats.getStats(cg));

}

   private static String prettyPrint(SSAInstruction inst) {
        //if (inst instanceof SSAInvokeInstruction)
	//  return inst.toString();
        Triple<Integer, CGNode, IExplodedBasicBlock> contextInfo = instructionContext.get(inst);
        int instIndex = ((Integer)contextInfo.val1).intValue();
        CGNode node = (CGNode)contextInfo.val2;        
        String instSt = inst.toString();
        int pos = instSt.indexOf("(");
        if (pos >= 0)
           instSt = instSt.substring(0,pos); 
        return instSt + " " + prettyPrint(node.getIR(), instIndex);  
    }

    private static String prettyPrint(IR ir, int instIndex) {
    String result="";
    try {

    IBytecodeMethod method = (IBytecodeMethod)ir.getMethod();
    IClass decclass =  method.getDeclaringClass();
    int bytecodeIndex = method.getBytecodeIndex(instIndex);
    int sourceLineNum = method.getLineNumber(bytecodeIndex);   
    result ="line " + sourceLineNum + " in " + (decclass.getSourceFileName() == null ? decclass.getName() : decclass.getSourceFileName());
    }
    catch(InvalidClassFileException e) {
       System.out.println(e);
       }
    catch(ClassCastException e) {
	result = "Fake class and method";
    }
    return result;
  }
  

  // Explore this particular node where methods are
  public static Integer explorePredecessors(Integer parentID, Integer statementCount, Integer kBranchCount, HashSet<IExplodedBasicBlock> visited, CGNode node, IExplodedBasicBlock origBB)
  {
    SSAInstruction orig = origBB.getInstruction();
    Integer myID = parentID + 1;
    if (((kDeep > 0) && (myID > kDeep)) || ((kBranch > 0) && (kBranchCount >= kBranch)))
    {
      return kBranchCount;
    }
    if (visited.contains(origBB))
    {
      //System.out.println("Visited the basic block before!");
      return kBranchCount;
    }
    visited.add(origBB);
   if (statementCount + 1 > maxStatementCount)
      maxStatementCount = statementCount + 1; 
    if (orig != null)
    {
      //System.out.println("Visiting " + prettyPrint(orig));
      if (orig instanceof SSAConditionalBranchInstruction)
      {
        SSAConditionalBranchInstruction ci = (SSAConditionalBranchInstruction) orig;
        controlStatements.add(ci);
        //System.out.println("Test: " + orig);
        kBranchCount++;
      }
      
      if (orig instanceof SSASwitchInstruction)
      {
        SSASwitchInstruction si = (SSASwitchInstruction) orig;
        controlStatements.add(si);
        //System.out.println("Test: " + orig);
        kBranchCount++;
      }
    }
      
      // Loop through preds and then call function recursively on each one.. Explores the predecessors

      // Finds the call sites of the method
     //System.out.println("\tBasicBlock's node= " + node);
     ExplodedControlFlowGraph graph = (ExplodedControlFlowGraph) icfg.getCFG(node);
     if ((graph != null) && (origBB != null))
     {

      Collection<IExplodedBasicBlock> preds = graph.getNormalPredecessors(origBB);      
      //System.out.println("Predecessors=" + preds.size());
      for(IExplodedBasicBlock predBB : preds) 
      {
            explorePredecessors(myID, statementCount + 1, kBranchCount, new HashSet<IExplodedBasicBlock>(visited), node, predBB);
      }
     }
    
    return kBranchCount;
  }
  
    private static void addToSet(HashMap<Object, HashSet<Object>> oneToMany, Object key, Object value) {
        HashSet<Object> set;
        if (oneToMany.containsKey(key))
            set  = oneToMany.remove(key);
        else
            set  = new HashSet<Object>();
        set.add(value);
        oneToMany.put(key, set);
    }

    private static void addCallSites(CGNode node, SSAInstruction inst) {
        if (inst instanceof SSAInvokeInstruction) {
            java.util.Set<CGNode> mnodes = cg.getNodes(((SSAInvokeInstruction)inst).getDeclaredTarget());
                
            //java.util.Set<CGNode> mnodes = cg.getNodes(((SSAInvokeInstruction)inst).getCallSite().getDeclaredTarget());

            OrdinalSet<? extends InstanceKey> lockSet = OrdinalSet.<InstanceKey>empty();
            //java.util.Set<CGNode> mnodes = getPossibleNodes(inst);
            for(CGNode n : mnodes) {
                
                   addToSet(callSites, n, inst);
                //System.out.println("HOW COME " + prettyPrint(inst) + "\n\t AND" + n.getMethod().getName().toString() + " ARE THE SAME?");
            }
        }
    }

  
  // Interprocedural control-flow analysis.. get to the next node where the method is called
  public static Integer explorePredecessorsInterProcedurally(Integer parentID, Integer statementCount, Integer kBranchCount, HashSet<CGNode> visited, CGNode current, SSAInstruction inst)
  {
    
    Integer myID = parentID + 1;
    if (((kDeep > 0) && (myID > kDeep)) || ((kBranch > 0) && (kBranchCount >= kBranch)))
    {
      return kBranchCount;
    }

    if (visited.contains(current))
    {
      //System.out.println("Node " + current + " visited!");
      return kBranchCount;
    }
    if (current.toString().indexOf("fakeRootMethod") >= 0) return kBranchCount;
    //System.out.println("Node " + current);
    //System.out.println("\t Instruction: " + inst); 
    visited.add(current);
    if (myID > maxNodePerPath)
       maxNodePerPath = myID;    
    maxStatementCount = 0;
    Triple<Integer, CGNode, IExplodedBasicBlock> contextInfo = instructionContext.get(inst);
    //CGNode node = (CGNode)contextInfo.val2;
    IExplodedBasicBlock bb = (IExplodedBasicBlock)contextInfo.val3;
    ExplodedControlFlowGraph graph = (ExplodedControlFlowGraph) icfg.getCFG(current);
    if ((graph != null) && (bb != null))
    {
      Collection<IExplodedBasicBlock> preds = graph.getNormalPredecessors(bb);

      for(IExplodedBasicBlock pred : preds)
      {
        kBranchCount = explorePredecessors(0, 0, kBranchCount, new HashSet<IExplodedBasicBlock>(), current, pred);
      }
    }
    
    if (statementCount + maxStatementCount > maxStatementCountAllPaths )
       maxStatementCountAllPaths = statementCount + maxStatementCount;
      
    HashSet<Object> csites = callSites.get(current);
    if (csites != null) {
        for(Object csins: csites) {
            SSAInvokeInstruction inst2 = (SSAInvokeInstruction) csins;
            CGNode res = (CGNode)instructionContext.get(inst2).val2;
            if (inst2.getDeclaredTarget().getName().toString().indexOf(current.getMethod().getName().toString()) >= 0) {
                //System.out.println("COMPARED : " + inst2.getDeclaredTarget().getName().toString() + " VS " + current.getMethod().getName().toString());
                //System.out.println("\tCall site: " + prettyPrint(inst2) + " in " + res);             
                kBranchCount = explorePredecessorsInterProcedurally(myID, statementCount + maxStatementCount, kBranchCount, new HashSet<CGNode>(visited), res, inst2);
            }
        }
    }
      
    return kBranchCount;
  }

    private static SSAInstruction findCallToMethodCall(String className, String methodName, String targetCl, String targetMt) throws InvalidClassFileException {
        System.out.println("Searching for instruction " + targetCl + "." + targetMt + "in " + className + "." + methodName);
        for(CGNode node: icfg.getCallGraph()) {
            if (node.getMethod().getDeclaringClass().getName().toString().indexOf(className) >= 0) {
                //System.out.println("Candidate class=" + node.getMethod().getDeclaringClass().getName().toString());
                //System.out.println("Is " + node.getMethod().getName().toString() + " what we're looking for?");
                if (node.getMethod().getName().toString().indexOf(methodName) >= 0) {
                    //System.out.println("Candidate method=" + node.getMethod().getName().toString());
                    IR ir = node.getIR();
                    if (ir == null) continue;
                    SSAInstruction[] insts = ir.getInstructions();
                    for(int i=0; i<insts.length; i++) {
                       if (insts[i] instanceof SSAInvokeInstruction) {
                           
                          MethodReference mr = ((SSAInvokeInstruction)insts[i]).getDeclaredTarget();
                         if (mr.getName().toString().indexOf(targetMt) >= 0 && mr.getDeclaringClass().getName().toString().indexOf(targetCl) >= 0)
                            return insts[i];
                       }
                    }
                }
            }
        }
        
        return null;
    }

  
    private static SSAInstruction findCallToInstrAtLine(String className, String methodName, int lineNo) throws InvalidClassFileException {
        System.out.println("Searching for instruction " + className + "." + methodName + " at line " + lineNo);
        for(CGNode node: icfg.getCallGraph()) {
            if (node.getMethod().getDeclaringClass().getName().toString().indexOf(className) >= 0) {
                //System.out.println("Candidate class=" + node.getMethod().getDeclaringClass().getName().toString());
                //System.out.println("Is " + node.getMethod().getName().toString() + " what we're looking for?");
                if (node.getMethod().getName().toString().indexOf(methodName) >= 0) {
                    //System.out.println("Candidate method=" + node.getMethod().getName().toString());
                    IR ir = node.getIR();
                    if (ir == null) continue;
                    SSAInstruction[] insts = ir.getInstructions();
                    for(int i=0; i < insts.length; i++) {
                        IBytecodeMethod method = (IBytecodeMethod)ir.getMethod();
                        int bytecodeIndex = method.getBytecodeIndex(i);
                        int sourceLineNum = method.getLineNumber(bytecodeIndex);
                        if (sourceLineNum == lineNo) {
                            return insts[i];
                        }
                    }
                }
            }
        }
        
        return null;
    }

  
  public static SSAInstruction findCallToInstr(CGNode n, String methodName) 
  {
     if (targetClassNames != null && targetClassNames.indexOf(n.getMethod().getDeclaringClass().getName().toString()) < 0)
     {
       return null;  
     }

     IR ir = n.getIR();
     if (ir == null) 
     {
       return null;
     }
     for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();)
     {
         SSAInstruction s = it.next();
         if (s instanceof SSAAbstractInvokeInstruction)
         {
           SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) s;
           if (call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)) 
           {
             return s;
           }
         }
     }
     //Assertions.UNREACHABLE("failed to find call to " + methodName + " in " + n);
     return null;
   }
  
  
  public static Statement createStatement(CGNode n, SSAInstruction inst) 
  {
     //System.out.println("Converting " + prettyPrint(inst) );
     if (targetClassNames != null && targetClassNames.indexOf(n.getMethod().getDeclaringClass().getName().toString()) < 0)
     {
       return null;  
     }

     IR ir = n.getIR();
     if (ir == null) 
     {
       return null;
     }
     for (int i = 0; i < ir.getInstructions().length; i++)
     {
         SSAInstruction s = ir.getInstructions()[i];

        if (s == inst) {
           return new NormalStatement(n, i);
        }
     }
     System.out.println("Could not locate the instruction in " + n);
     return null;
   }
  
  
  public static void dumpSlice(Statement seed, Collection<Statement> slice) 
  {
      for (Statement s : slice) 
      {
          if (s.getKind() == Statement.Kind.NORMAL_RET_CALLER) // ignore special kinds of statements
          {
             SSAAbstractInvokeInstruction inst = ((NormalReturnCaller)s).getInstruction();

             if (inst.getDeclaredTarget().getName().toString().indexOf("getInt") >= 0 
	                      && (inst.getDeclaredTarget().getDeclaringClass().getName().toString().indexOf("Lorg/apache/hadoop/conf/Configuration") >=0 
	       || inst.getDeclaredTarget().getDeclaringClass().getName().toString().indexOf("Lorg/apache/hadoop/mapred/JobConf") >=0) 
	       && inst.getDeclaredTarget().getDescriptor().toString().indexOf("Ljava/lang/String;") >= 0) 
             //if (inst.getDeclaredTarget().getName().toString().indexOf("getInt") >= 0 
               //  && inst.getDeclaredTarget().getDeclaringClass().getName().toString().indexOf(filterClass) >=0 
		 //    && inst.getDeclaredTarget().getDescriptor().toString().indexOf(filterParType) >= 0)
             {   
               //System.out.println();
               //System.out.println(s);

             int bcIndex, instructionIndex = ((StatementWithInstructionIndex) s).getInstructionIndex();
             try {
                   bcIndex = ((ShrikeBTMethod) s.getNode().getMethod()).getBytecodeIndex(instructionIndex);
                   try {
                         int src_line_number = s.getNode().getMethod().getLineNumber(bcIndex);
                         //System.out.println ( "Source line number = " + src_line_number + " in class " + s.getNode().getMethod().getDeclaringClass());
                         
                         SymbolTable tbl = s.getNode().getIR().getSymbolTable();
                         for (int i = 0; i < inst.getNumberOfParameters(); i++)
                         {
                           int paramValueNum = inst.getUse(i);
                           if (tbl.isStringConstant(paramValueNum)) 
                           {
                             String configParam = tbl.getStringValue(paramValueNum);
                             System.out.println("Configuration Parameters: " + configParam);
                           }
                         }
                   } 
                   catch (Exception e) 
                   {
                     System.out.println("Bytecode index no good" + " in class " + s.getNode().getMethod().getDeclaringClass());
                     System.out.println("ERROR: " + e.getMessage());
                   }
             } 
             catch (Exception e)
             {
                 System.out.println("it's probably not a BT method (e.g. it's a fakeroot method)" + " in class " + s.getNode().getMethod().getDeclaringClass());
                 System.out.println(e.getMessage());
             } 
             }
             }
          }
    }
  
  
  private static boolean isAnEntryClass(String name) 
  {
    String[] entryClassName = entryClass.split(";");
    for(int i=0; i < entryClassName.length; i++)
    {
       if (name.indexOf(entryClassName[i]) >= 0)
       {
          return true;
       }
    }
    return false;
  }
  
  private static boolean isATarget(CGNode node) 
  {
      String className = node.getMethod().getDeclaringClass().getName().toString(); 
      if (mainClass != null && className.indexOf(mainClass) >= 0)
          return true;
      if (entryClass != null && isAnEntryClass(className))
          return true;  
      if (targetClassNames == null) // All classes will be analyzed
          return true; 
      String[] targetClassName = targetClassNames.split(";");        
      for(int i=0; i < targetClassName.length; i++) 
      {
        if (className.indexOf(targetClassName[i]) >= 0)
        return true;
      }
      return false; 
  }
  
  private static void configureAndCreateCallGraph(String scopeFile, String mainClass, String entryClass) throws IOException, ClassHierarchyException, CallGraphBuilderCancelException  {
    File exclusionsFile = null;
    AnalysisScope scope = AnalysisScopeReader.readJavaScope(scopeFile, exclusionsFile, HadoopAnalyzer.class.getClassLoader()); 
    cha = ClassHierarchy.make(scope);
    //System.out.println(cha.getNumberOfClasses() + " classes");
    //System.out.println(Warnings.asString());
    Warnings.clear();
    AnalysisOptions options = new AnalysisOptions();
    Iterable<Entrypoint> entrypoints = null;
    if (entryClass != null) {
       ArrayList<Entrypoint> all = new ArrayList<Entrypoint>();
       String[] entryClassName = entryClass.split(";");
       for(int i=0; i < entryClassName.length; i++) {
          System.out.println("Making entry points for class " + entryClassName[i]);
          // Removing the prefix L before passing to makePublicEntryPoints!
          all.addAll((Collection<Entrypoint>)makePublicEntrypoints(scope, cha, entryClassName[i].substring(1)));
      }
       entrypoints = all;
    }
    else entrypoints = Util.makeMainEntrypoints(scope, cha, mainClass);
    //Iterable<Entrypoint> entrypoints = entryClass != null ? makePublicEntrypoints(scope, cha, entryClass) : Util.makeMainEntrypoints(scope, cha, mainClass);
    options.setEntrypoints(entrypoints);
    options.setHandleStaticInit(true);
    // you can dial down reflection handling if you like
    options.setReflectionOptions(AnalysisOptions.ReflectionOptions.NONE);
    AnalysisCache cache = new AnalysisCache();
    // other builders can be constructed with different Util methods
    Util.addDefaultSelectors(options, cha);
    Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);
    ContextSelector appSelector = null;
    SSAContextInterpreter appInterpreter = null;
  
    // This disables ZeroXInstanceKeys.SMUSH_PRIMITIVE_HOLDERS (by not explicitly specifying it)
    if (pType.equals("zeroOneConCFA"))
       builder = new ZeroXContainerCFABuilder(cha, options, cache, appSelector, appInterpreter, ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.SMUSH_MANY |  ZeroXInstanceKeys.SMUSH_STRINGS | ZeroXInstanceKeys.SMUSH_THROWABLES);
    else if (pType.equals("zeroOneCFA"))
       builder = new ZeroXCFABuilder(cha, options, cache, appSelector, appInterpreter, ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.SMUSH_MANY |  ZeroXInstanceKeys.SMUSH_STRINGS | ZeroXInstanceKeys.SMUSH_THROWABLES);
    else if (pType.equals("zeroCFA"))
       builder = new ZeroXCFABuilder(cha, options, cache, appSelector, appInterpreter, ZeroXInstanceKeys.NONE);
    else if (pType.equals("oneCFA")) {
        builder = new nCFABuilder(1, cha, options, cache, appSelector, appInterpreter);
        ((PropagationCallGraphBuilder)builder).setInstanceKeys(new ZeroXInstanceKeys(options, cha, ((PropagationCallGraphBuilder)builder).getContextInterpreter(), ZeroXInstanceKeys.ALLOCATIONS
        | ZeroXInstanceKeys.SMUSH_MANY | ZeroXInstanceKeys.SMUSH_STRINGS
        | ZeroXInstanceKeys.SMUSH_THROWABLES));
    }
    else if (pType.equals("twoCFA")) {
        builder = new nCFABuilder(2, cha, options, cache, appSelector, appInterpreter);
        ((PropagationCallGraphBuilder)builder).setInstanceKeys(new ZeroXInstanceKeys(options, cha, ((PropagationCallGraphBuilder)builder).getContextInterpreter(), ZeroXInstanceKeys.ALLOCATIONS
        | ZeroXInstanceKeys.SMUSH_MANY | ZeroXInstanceKeys.SMUSH_STRINGS
        | ZeroXInstanceKeys.SMUSH_THROWABLES));
    }
  
    
    //Util.makeZeroOneContainerCFABuilder(options, cache, cha, scope);     
    cg = builder.makeCallGraph(options, null);

}

  
  private static Iterable<Entrypoint> makePublicEntrypoints(AnalysisScope scope, IClassHierarchy cha, String entryClass) 
  {
    Collection<Entrypoint> result = new ArrayList<Entrypoint>();
    System.out.println(StringStuff.deployment2CanonicalTypeString(entryClass));
    System.out.println(TypeReference.findOrCreate(ClassLoaderReference.Application,
        StringStuff.deployment2CanonicalTypeString(entryClass)));
    IClass klass = cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Application,
        StringStuff.deployment2CanonicalTypeString(entryClass)));
    for (IMethod m : klass.getDeclaredMethods()) 
    {
      //System.out.println("Adding method " + m + " as an entry point");
      //if (m.isPublic()) {
        result.add(new DefaultEntrypoint(m, cha));
      //}
    }
    return result;
  }
  
}
