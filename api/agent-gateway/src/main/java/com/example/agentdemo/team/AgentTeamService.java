package com.example.agentdemo.team;

import com.example.agentdemo.model.ChatRequest;
import com.example.agentdemo.service.AgentService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * 主管 Agent 负责路由，专家 Agent 只执行自己的工具白名单。
 *
 * 这不是不受控的 Agent 群聊，而是可审计的 supervisor -> specialist 工作流。
 */
@Service
public class AgentTeamService {

    private static final String SUPERVISOR = "supervisor";

    private final SpecialistAgentRegistry registry;
    private final AgentService agentService;

    public AgentTeamService(SpecialistAgentRegistry registry, AgentService agentService) {
        this.registry = registry;
        this.agentService = agentService;
    }

    public AgentTeamResponse chat(ChatRequest request) {
        Route route = route(request);
        Set<String> allowedTools = route.agent == null
                ? Collections.<String>emptySet()
                : route.agent.getAllowedTools();

        Collection<AgentHandoff> handoffs = route.agent == null
                ? Collections.<AgentHandoff>emptyList()
                : Collections.singletonList(new AgentHandoff(SUPERVISOR, route.agent.getCode(), route.reason));

        return new AgentTeamResponse(
                route.agent == null ? SUPERVISOR : route.agent.getCode(),
                route.reason,
                new java.util.ArrayList<AgentHandoff>(handoffs),
                agentService.chat(request, allowedTools)
        );
    }

    public Collection<SpecialistAgent> agents() {
        return registry.all();
    }

    private Route route(ChatRequest request) {
        if (!isBlank(request.getConfirmDraftId())) {
            SpecialistAgent purchase = registry.find("purchase-specialist");
            return new Route(purchase, "收到显式 confirmDraftId，交给采购专家执行人工确认后的写操作");
        }

        SpecialistAgent explicit = registry.find(request.getAgentCode());
        if (explicit != null) {
            return new Route(explicit, "请求指定了已注册的专家 Agent: " + explicit.getCode());
        }

        String query = request.getQuery() == null ? "" : request.getQuery();
        if (containsAny(query, "采购", "购买", "补货", "草稿", "下单")) {
            return new Route(registry.find("purchase-specialist"), "检测到采购或草稿意图");
        }
        if (containsAny(query, "库存", "查询", "剩余", "还有多少")) {
            return new Route(registry.find("inventory-specialist"), "检测到库存查询意图");
        }
        return new Route(null, "未识别到可委派的库存或采购任务，由主管请求用户补充信息");
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class Route {
        private final SpecialistAgent agent;
        private final String reason;

        private Route(SpecialistAgent agent, String reason) {
            this.agent = agent;
            this.reason = reason;
        }
    }
}
