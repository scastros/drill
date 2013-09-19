/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.optiq;

import org.eigenbase.rel.RelCollationImpl;
import org.eigenbase.rel.RelNode;
import org.eigenbase.rel.SortRel;
import org.eigenbase.relopt.Convention;
import org.eigenbase.relopt.RelOptRule;
import org.eigenbase.relopt.RelOptRuleCall;
import org.eigenbase.relopt.RelTraitSet;

/**
 * This rule converts a SortRel that has either a offset and fetch into a Drill Sort and Limit Rel
 */
public class DrillLimitRule extends RelOptRule {
  public static DrillLimitRule INSTANCE = new DrillLimitRule();

  private DrillLimitRule() {
    super(RelOptRule.some(SortRel.class, Convention.NONE, RelOptRule.any(RelNode.class)), "DrillLimitRule");
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    final SortRel sort = call.rel(0);
    if (sort.offset == null && sort.fetch == null) {
      return;
    }
    final RelTraitSet traits = sort.getTraitSet().plus(DrillRel.CONVENTION);
    RelNode input = sort.getChild();
    if (!sort.getCollation().getFieldCollations().isEmpty()) {
      input = sort.copy(
          sort.getTraitSet().replace(RelCollationImpl.EMPTY),
          input,
          RelCollationImpl.EMPTY,
          null,
          null);
    }
    RelNode x = convert(
        input,
        input.getTraitSet().replace(DrillRel.CONVENTION));
    call.transformTo(new DrillLimitRel(sort.getCluster(), traits, x, sort.offset, sort.fetch));
  }
}