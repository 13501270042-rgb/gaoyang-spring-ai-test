package com.kakuiwong.gaoyangspringai.ai.agent;

/**
 * <h2>Handoff（任务交接）多Agent模式 知识点总结</h2>
 *
 * <p>基于 alibaba-cloud-ai-graph 框架，实现多Agent协作的“路由分发”架构。</p>
 *
 * <h3>核心概念</h3>
 * <ul>
 *   <li><b>ReactAgent</b>：具备 ReAct（Reasoning + Acting）能力的子Agent，
 *       能自主推理并调用工具完成任务。通过 builder 模式构建，需指定 name、model、
 *       description、instruction、methodTools、hooks 等属性。</li>
 *   <li><b>LlmRoutingAgent</b>：路由Agent，由大模型根据用户意图从多个子Agent中
 *       选择最合适的一个来执行任务。它是整个 Handoff 模式的“调度中枢”。</li>
 *   <li><b>ModelCallLimitHook</b>：调用次数限制钩子，通过 runLimit 限制单次请求中
 *       Agent 调用大模型的次数，防止无限循环或过度消耗。</li>
 * </ul>
 *
 * <h3>架构设计</h3>
 * <pre>
 * 用户请求 -&gt; LlmRoutingAgent（路由层）
 *              |-- foodAgent（美食专家）
 *              |-- travelAgent（旅游专家）
 * </pre>
 * <p>路由Agent通过 systemPrompt 定义判断逻辑，根据关键词将请求分发给唯一子Agent，
 * 禁止并行分发到多个Agent。</p>
 *
 * <h3>关键配置要点</h3>
 * <ul>
 *   <li>每个 ReactAgent 的 description 必须清晰界定职责边界（含“不处理”排除声明），
 *       这是路由Agent做决策的依据。</li>
 *   <li>子Agent通过 methodTools 注入工具（如 WebSearchTool），
 *       实现“先搜索再回答”的工作流。</li>
 *   <li>instruction 中明确要求“优先使用工具，不要凭记忆回答”，
 *       保证回答的时效性。</li>
 *   <li>路由Agent的 runLimit 通常设置比子Agent更高（如3），
 *       因为路由本身也需要一次模型调用来做分发决策。</li>
 *   <li>路由Agent通过 subAgents(List.of(...)) 注册所有子Agent。</li>
 *   <li>LlmRoutingAgent 是单轮路由设计，不支持多轮循环（不同于 SupervisorAgent）；
 *       限制调用次数用 ModelCallLimitHook，而非 maxIterations（该方法不存在于路由Agent）。</li>
 * </ul>
 *
 * <h3>适用场景</h3>
 * <p>当业务领域可明确划分为多个独立子领域时（如美食、旅游、住宿等），
 * 使用 Handoff 模式让每个子Agent专注自身领域，由路由Agent统一调度，
 * 实现职责分离和专业化处理。</p>
 *
 * @see com.alibaba.cloud.ai.graph.agent.ReactAgent
 * @see com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent
 * @see com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook
 */