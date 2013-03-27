package nl.vu.cs.querypie.reasoner.actions;

import java.util.ArrayList;
import java.util.List;

import nl.vu.cs.ajira.actions.Action;
import nl.vu.cs.ajira.actions.ActionConf;
import nl.vu.cs.querypie.reasoner.actions.io.WriteDerivationsBtree;
import nl.vu.cs.querypie.reasoner.actions.io.WriteInMemory;
import nl.vu.cs.querypie.reasoner.actions.rules.GenericRuleExecutor;
import nl.vu.cs.querypie.reasoner.common.Consts;
import nl.vu.cs.querypie.reasoner.common.ParamHandler;

public abstract class AbstractRulesController extends Action {
	protected void applyRulesSchemaOnly(List<ActionConf> actions, boolean writeToBTree, int step, boolean flaggedOnly) {
		ParallelExecutionSchemaOnly.addToChain(step - 3, actions);
		ActionsHelper.sort(actions, false);
		if (ParamHandler.get().isUsingCount()) {
			AddDerivationCount.addToChain(actions, false);
		} else {
			ActionsHelper.removeDuplicates(actions);
		}
		if (writeToBTree) {
			WriteDerivationsBtree.addToChain(true, step, actions);
		} else {
			WriteInMemory.addToChain(actions, Consts.CURRENT_DELTA_KEY);
		}
		ActionsHelper.collectToNode(actions);
		ReloadSchema.addToChain(actions, false);
	}

	protected void applyRulesWithGenericPatterns(List<ActionConf> actions, boolean writeToBTree, int step, boolean flaggedOnly) {
		ActionsHelper.readEverythingFromBTree(actions);
		ActionsHelper.reconnectAfter(3, actions);
		GenericRuleExecutor.addToChain(step, actions);
		SetStep.addToChain(step, actions);
		ActionsHelper.reconnectAfter(4, actions);
		ActionsHelper.mapReduce(actions, step - 2, false);
		ActionsHelper.sort(actions, true);
		if (ParamHandler.get().isUsingCount()) {
			AddDerivationCount.addToChain(actions, true);
		} else {
			ActionsHelper.removeDuplicates(actions);
		}
		if (writeToBTree) {
			WriteDerivationsBtree.addToChain(false, step, actions);
		} else {
			WriteInMemory.addToChain(actions, Consts.CURRENT_DELTA_KEY);
		}
	}

	protected void applyRulesWithGenericPatternsInABranch(List<ActionConf> actions, boolean writeToBTree, int step, boolean flaggedOnly) {
		List<ActionConf> actions2 = new ArrayList<ActionConf>();
		applyRulesWithGenericPatterns(actions2, writeToBTree, step, flaggedOnly);
		ActionsHelper.createBranch(actions, actions2);
	}

}
