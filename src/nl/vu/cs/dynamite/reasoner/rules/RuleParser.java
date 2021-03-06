package nl.vu.cs.dynamite.reasoner.rules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import nl.vu.cs.dynamite.reasoner.support.Utils;
import nl.vu.cs.dynamite.storage.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuleParser {

  static final Logger log = LoggerFactory.getLogger(RuleParser.class);

  public List<Rule> parseRules(String file) throws Exception {
    log.info("Start parsing the ruleset file");
    List<Rule> output = new ArrayList<Rule>();

    BufferedReader f = new BufferedReader(new FileReader(new File(file)));
    String line = f.readLine();
    int i = 0;

    while (line != null) {
      String[] split = line.split(" :- ");
      String head = split[0];
      String[] sBody = split[1].split(",");
      Pattern[] body = new Pattern[sBody.length];
      for (int j = 0; j < sBody.length; ++j) {
        body[j] = Utils.parsePattern(sBody[j]);
      }
      Rule rule = new Rule(i, Utils.parsePattern(head), body);
      output.add(rule);
      line = f.readLine();
      i++;
    }
    f.close();

    return output;
  }
}
