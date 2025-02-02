package moe.knox.factorio.intellij.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.tang.intellij.lua.psi.LuaIndexExpr;
import com.tang.intellij.lua.psi.LuaLiteralExpr;
import com.tang.intellij.lua.psi.LuaTableField;
import com.tang.intellij.lua.psi.LuaTypes;
import moe.knox.factorio.intellij.completion.factorio.condition.PathPatternCondition;
import moe.knox.factorio.intellij.completion.factorio.condition.PrototypePatternCondition;
import moe.knox.factorio.intellij.completion.factorio.condition.PrototypeTypePatternCondition;
import moe.knox.factorio.intellij.completion.factorio.provider.PathCompletionProvider;
import moe.knox.factorio.intellij.completion.factorio.provider.PrototypeCompletionProvider;
import moe.knox.factorio.intellij.completion.factorio.provider.PrototypeTableCompletionProvider;
import moe.knox.factorio.intellij.completion.factorio.provider.PrototypeTypeCompletionProvider;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class MainCompletionContributor extends CompletionContributor {
    public MainCompletionContributor() {
        // autocomplete "type" field inside "data:extend"
        extend(CompletionType.BASIC,
                psiElement()
                        .with(new FactorioIntegrationActiveCondition(null))
                        .andOr(
                                psiElement()
                                        .withParent(
                                                psiElement(LuaTypes.NAME_EXPR)
                                                        .withParent(LuaTableField.class)
                                        )
                        ).with(new PrototypePatternCondition(null, true)),
                new PrototypeCompletionProvider()
        );

        // autocomplete string literal for "type" in "data:extend"
        extend(CompletionType.BASIC,
                psiElement(LuaTypes.STRING)
                        .with(new FactorioIntegrationActiveCondition(null))
                        .with(new PrototypePatternCondition(null, false))
                        .with(new PrototypeTypePatternCondition(null)),
                new PrototypeTypeCompletionProvider());

        // autocomplete paths
        extend(CompletionType.BASIC,
                psiElement(LuaTypes.STRING)
                        .with(new FactorioIntegrationActiveCondition(null))
                        .with(new PathPatternCondition()),
                new PathCompletionProvider()
        );

        // autocomplete recipe and technology strings from PrototypeIndexer
        extend(CompletionType.BASIC,
                psiElement(LuaTypes.STRING)
                        .withParent(
                                psiElement(LuaLiteralExpr.class)
                                        .withParent(LuaIndexExpr.class)
                        )
                        .with(new FactorioIntegrationActiveCondition(null)),
                new PrototypeTableCompletionProvider()
        );
    }
}
