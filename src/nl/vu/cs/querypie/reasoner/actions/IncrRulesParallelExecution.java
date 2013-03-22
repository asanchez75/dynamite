package nl.vu.cs.querypie.reasoner.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import nl.vu.cs.ajira.actions.Action;
import nl.vu.cs.ajira.actions.ActionConf;
import nl.vu.cs.ajira.actions.ActionContext;
import nl.vu.cs.ajira.actions.ActionOutput;
import nl.vu.cs.ajira.data.types.Tuple;
import nl.vu.cs.querypie.ReasoningContext;
import nl.vu.cs.querypie.reasoner.common.Consts;
import nl.vu.cs.querypie.reasoner.rules.Rule;
import nl.vu.cs.querypie.storage.Pattern;
import nl.vu.cs.querypie.storage.Term;
import nl.vu.cs.querypie.storage.inmemory.InMemoryTupleSet;
import nl.vu.cs.querypie.storage.inmemory.Tuples;

public class IncrRulesParallelExecution extends Action {

  @Override
  public void process(Tuple tuple, ActionContext context, ActionOutput actionOutput) throws Exception {

  }

  @Override
  public void stopProcess(ActionContext context, ActionOutput actionOutput) throws Exception {
    List<Integer> rulesOnlySchema = new ArrayList<Integer>();
    List<Rule> rulesSchemaGenerics = new ArrayList<Rule>();

    // Determine the rules that have information in delta and organize them according to their type
    extractSchemaRulesWithInformationInDelta(context, rulesOnlySchema, rulesSchemaGenerics);

    // Execute all schema rules in parallel (on different branches)
    ActionsHelper.parallelRunPrecomputedRuleExecutorForRules(rulesOnlySchema, true, actionOutput);

    // Read all the delta triples and apply all the rules with a single antecedent
    executeGenericRules(context, actionOutput);

    // Execute rules that require a map and a reduce
    // FIXME This operation is necessary, but is this the right place to perform it?
    ActionsHelper.reloadPrecomputationOnRules(rulesSchemaGenerics, context, true);
    executePrecomGenericRules(context, actionOutput);

    // If some schema is changed, re-apply the rules over the entire input which is affected
    for (Rule rule : rulesSchemaGenerics) {
      Pattern pattern = getQueryPattern(rule);
      Collection<Long> possibleValues = getValuesMatchingTheSchema(rule);
      int varPos = getVariablePosition(rule);
      for (long v : possibleValues) {
        pattern.setTerm(varPos, new Term(v));
        executePrecomGenericRulesForPattern(pattern, context, actionOutput);
      }
    }

  }

  private void extractSchemaRulesWithInformationInDelta(ActionContext context, List<Integer> rulesOnlySchema, List<Rule> rulesSchemaGenerics) throws Exception {
    InMemoryTupleSet set = (InMemoryTupleSet) context.getObjectFromCache(Consts.CURRENT_DELTA_KEY);
    Map<Pattern, Collection<Rule>> patterns = ReasoningContext.getInstance().getRuleset().getPrecomputedPatternSet();
    List<Rule> allSchemaOnlyRules = ReasoningContext.getInstance().getRuleset().getAllSchemaOnlyRules();
    List<Rule> selectedSchemaOnlyRules = new ArrayList<Rule>();
    for (Pattern p : patterns.keySet()) {
      // Skip if it does not include schema information
      if (set.getSubset(p).isEmpty()) {
        continue;
      }
      for (Rule rule : patterns.get(p)) {
        if (rule.getGenericBodyPatterns().isEmpty()) {
          selectedSchemaOnlyRules.add(rule);
        } else {
          rulesSchemaGenerics.add(rule);
        }
      }
    }
    for (int i = 0; i < allSchemaOnlyRules.size(); ++i) {
      Rule r = allSchemaOnlyRules.get(i);
      if (selectedSchemaOnlyRules.contains(r)) {
        rulesOnlySchema.add(i);
      }
    }
  }

  private void executeGenericRules(ActionContext context, ActionOutput actionOutput) throws Exception {
    List<ActionConf> actions = new ArrayList<ActionConf>();
    ActionsHelper.readFakeTuple(actions);
    ActionsHelper.runReadAllInMemoryTuples(actions);
    ActionsHelper.runGenericRuleExecutor(actions);
    actionOutput.branch(actions);
  }

  private void executePrecomGenericRules(ActionContext context, ActionOutput actionOutput) throws Exception {
    List<ActionConf> actions = new ArrayList<ActionConf>();
    ActionsHelper.readFakeTuple(actions);
    ActionsHelper.runReadAllInMemoryTuples(actions);
    ActionsHelper.runMapReduce(actions, true);
    actionOutput.branch(actions);
  }

  private void executePrecomGenericRulesForPattern(Pattern pattern, ActionContext context, ActionOutput actionOutput) throws Exception {
    List<ActionConf> actions = new ArrayList<ActionConf>();
    ActionsHelper.runReadFromBTree(pattern, actions);
    ActionsHelper.runMapReduce(actions, true);
    actionOutput.branch(actions);
  }

  private Pattern getQueryPattern(Rule rule) {
    Pattern pattern = new Pattern();
    rule.getGenericBodyPatterns().get(0).copyTo(pattern);
    return pattern;
  }

  private Collection<Long> getValuesMatchingTheSchema(Rule rule) {
    int[][] shared_pos = rule.getSharedVariablesGen_Precomp();
    Tuples tuples = rule.getFlaggedPrecomputedTuples();
    return tuples.getSortedSet(shared_pos[0][1]);
  }

  private int getVariablePosition(Rule rule) {
    int[][] shared_pos = rule.getSharedVariablesGen_Precomp();
    return shared_pos[0][0];
  }
}