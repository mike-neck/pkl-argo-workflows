@import com.github.mikeneck.pkl.argo.workflows.core.AliasDefinition
@param AliasDefinition alias

@for(var kind: alias.getAlias().getKind().getDependencyNames())import ".../${kind}"
@endfor

!{var comment = alias.getDescription();} @for(var c: comment.makeLines(alias.getSimpleName()))/// ${c}
@endfor
typealias ${alias.getSimpleName()} = ${alias.getAlias().getKind().getSimple()}
