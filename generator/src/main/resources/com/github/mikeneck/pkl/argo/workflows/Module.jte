@import com.github.mikeneck.pkl.argo.workflows.pkl.PklModule
@param PklModule pkl
@for(var comment: pkl.getComments())/// ${comment}
@endfor
open module ${pkl.getTypeName()}

@for(var type: pkl.getImports())import ".../${type}"
@endfor

@for(var property: pkl.getProperties())
@for(var comment: property.getCommentLines())/// ${comment}
@endfor
${property.getDefinitionLine()}
@endfor

@for(var alias: pkl.getAliasesDef())
@for(var comment: alias.getComments())/// ${comment}
@endfor
@for(var line: alias.code(false))${line}
@endfor
@endfor
