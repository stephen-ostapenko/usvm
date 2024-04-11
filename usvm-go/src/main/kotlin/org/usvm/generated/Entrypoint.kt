package org.usvm.generated

import java.io.BufferedReader

val ptrMap: MutableMap<Int, Any> = mutableMapOf()
val structToPtrMap: MutableMap<Any, Int> = mutableMapOf()
val ptrToJacoMap: MutableMap<Int, Any> = mutableMapOf()
val mapDec: Map<String, (BufferedReader, Int)->Any?> = mapOf(
    "Int" to ::readInteger,
    "Short" to ::readInteger,
    "Long" to ::readInteger,
    "Float" to ::readReal,
    "Double" to ::readReal,
    "String" to ::readString,
    "Boolean" to ::readBoolean,
    "nil" to ::readNil,

    "array" to ::readArray,
    "map" to ::readMap,
	"ssa_BasicBlock" to ::read_ssa_BasicBlock,
	"token_lineInfo" to ::read_token_lineInfo,
	"ast_BasicLit" to ::read_ast_BasicLit,
	"types_TypeParamList" to ::read_types_TypeParamList,
	"types_Info" to ::read_types_Info,
	"types_Instance" to ::read_types_Instance,
	"types_TypeName" to ::read_types_TypeName,
	"types_Term" to ::read_types_Term,
	"ssa_domInfo" to ::read_ssa_domInfo,
	"sync_RWMutex" to ::read_sync_RWMutex,
	"sync_Mutex" to ::read_sync_Mutex,
	"ssa_lblock" to ::read_ssa_lblock,
	"ssa_Global" to ::read_ssa_Global,
	"token_File" to ::read_token_File,
	"types_Selection" to ::read_types_Selection,
	"ast_Comment" to ::read_ast_Comment,
	"types_object" to ::read_types_object,
	"types_Context" to ::read_types_Context,
	"token_FileSet" to ::read_token_FileSet,
	"ssa_Parameter" to ::read_ssa_Parameter,
	"types_Initializer" to ::read_types_Initializer,
	"types_version" to ::read_types_version,
	"ast_FieldList" to ::read_ast_FieldList,
	"types_Func" to ::read_types_Func,
	"ast_Ident" to ::read_ast_Ident,
	"types_Pointer" to ::read_types_Pointer,
	"ssa_Package" to ::read_ssa_Package,
	"typeutil_Map" to ::read_typeutil_Map,
	"types_Interface" to ::read_types_Interface,
	"types_Config" to ::read_types_Config,
	"types_Basic" to ::read_types_Basic,
	"typeutil_Hasher" to ::read_typeutil_Hasher,
	"typeutil_entry" to ::read_typeutil_entry,
	"types_Package" to ::read_types_Package,
	"types_MethodSet" to ::read_types_MethodSet,
	"ssa_tpWalker" to ::read_ssa_tpWalker,
	"types_Const" to ::read_types_Const,
	"ssa_If" to ::read_ssa_If,
	"ast_Field" to ::read_ast_Field,
	"ast_File" to ::read_ast_File,
	"types_monoGraph" to ::read_types_monoGraph,
	"generatedInlineStruct_001" to ::read_generatedInlineStruct_001,
	"ssa_register" to ::read_ssa_register,
	"types__TypeSet" to ::read_types__TypeSet,
	"types_monoEdge" to ::read_types_monoEdge,
	"ast_FuncDecl" to ::read_ast_FuncDecl,
	"types_Named" to ::read_types_Named,
	"ast_Scope" to ::read_ast_Scope,
	"types_Nil" to ::read_types_Nil,
	"ssa_Program" to ::read_ssa_Program,
	"ast_SelectorExpr" to ::read_ast_SelectorExpr,
	"types_exprInfo" to ::read_types_exprInfo,
	"ssa_BinOp" to ::read_ssa_BinOp,
	"types_TypeList" to ::read_types_TypeList,
	"types_Union" to ::read_types_Union,
	"ssa_Function" to ::read_ssa_Function,
	"types_actionDesc" to ::read_types_actionDesc,
	"ssa_selection" to ::read_ssa_selection,
	"types_Var" to ::read_types_Var,
	"types_Tuple" to ::read_types_Tuple,
	"atomic_Int32" to ::read_atomic_Int32,
	"types_PkgName" to ::read_types_PkgName,
	"types_ctxtEntry" to ::read_types_ctxtEntry,
	"ast_IfStmt" to ::read_ast_IfStmt,
	"types_instanceLookup" to ::read_types_instanceLookup,
	"ssa_FreeVar" to ::read_ssa_FreeVar,
	"ssa_canonizer" to ::read_ssa_canonizer,
	"ast_CommentGroup" to ::read_ast_CommentGroup,
	"ast_BlockStmt" to ::read_ast_BlockStmt,
	"types_instance" to ::read_types_instance,
	"types_action" to ::read_types_action,
	"ssa_Const" to ::read_ssa_Const,
	"ast_Object" to ::read_ast_Object,
	"ast_FuncType" to ::read_ast_FuncType,
	"types_Builtin" to ::read_types_Builtin,
	"ssa_CallCommon" to ::read_ssa_CallCommon,
	"ssa_targets" to ::read_ssa_targets,
	"ssa_Call" to ::read_ssa_Call,
	"types_TypeParam" to ::read_types_TypeParam,
	"types_monoVertex" to ::read_types_monoVertex,
	"ssa_UnOp" to ::read_ssa_UnOp,
	"ssa_Store" to ::read_ssa_Store,
	"types_declInfo" to ::read_types_declInfo,
	"ast_TypeSpec" to ::read_ast_TypeSpec,
	"types_Checker" to ::read_types_Checker,
	"types_dotImportKey" to ::read_types_dotImportKey,
	"ssa_Jump" to ::read_ssa_Jump,
	"ast_CallExpr" to ::read_ast_CallExpr,
	"ssa_Return" to ::read_ssa_Return,
	"types_Label" to ::read_types_Label,
	"types_importKey" to ::read_types_importKey,
	"ast_ImportSpec" to ::read_ast_ImportSpec,
	"types_environment" to ::read_types_environment,
	"ssa_Alloc" to ::read_ssa_Alloc,
	"generatedInlineStruct_000" to ::read_generatedInlineStruct_000,
	"ssa_subster" to ::read_ssa_subster,
	"types_Scope" to ::read_types_Scope,
	"types_TypeAndValue" to ::read_types_TypeAndValue,
	"typeutil_MethodSetCache" to ::read_typeutil_MethodSetCache,
	"ast_ExprStmt" to ::read_ast_ExprStmt,
	"ssa_generic" to ::read_ssa_generic,
	"sync_Once" to ::read_sync_Once,
	"types_Signature" to ::read_types_Signature,
	"ssa_typeListMap" to ::read_ssa_typeListMap,
	"ast_ReturnStmt" to ::read_ast_ReturnStmt,
	"types_term" to ::read_types_term,
	"ast_BinaryExpr" to ::read_ast_BinaryExpr
)

fun StartDeserializer(buffReader: BufferedReader): Any? {
    val line = buffReader.readLine()
    val split = line.split(" ")
    val readType = split[0]
    var id = -1
    if (split.size > 1) {
        id = split[1].toInt()
    }
    return mapDec[readType]?.invoke(buffReader, id)
}
