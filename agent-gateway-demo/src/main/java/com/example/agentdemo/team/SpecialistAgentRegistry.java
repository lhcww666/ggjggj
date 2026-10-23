package com.example.agentdemo.team;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SpecialistAgentRegistry {

    private final Map<String, SpecialistAgent> agents = new LinkedHashMap<String, SpecialistAgent>();

    public SpecialistAgentRegistry() {
        register(new SpecialistAgent(
                "inventory-specialist",
                "库存专家",
                "只负责按物料编码查询实时库存。",
                "query_stock"
        ));
        register(new SpecialistAgent(
                "purchase-specialist",
                "采购专家",
                "负责创建采购草稿，并在用户显式确认后创建正式采购订单。",
                "prepare_purchase_draft",
                "confirm_purchase_draft"
        ));
    }

    public SpecialistAgent find(String code) {
        if (code == null) {
            return null;
        }
        return agents.get(code.trim());
    }

    public Collection<SpecialistAgent> all() {
        return Collections.unmodifiableCollection(agents.values());
    }

    private void register(SpecialistAgent agent) {
        if (agents.put(agent.getCode(), agent) != null) {
            throw new IllegalStateException("重复的专家 Agent: " + agent.getCode());
        }
    }
}
