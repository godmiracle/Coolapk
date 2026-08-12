# AI Agent Instructions

默认使用简体中文进行分析、说明、待办记录和最终总结；代码、命令、文件名及错误原文可保留英文。

本文件是给 Codex / ChatGPT / Claude Code / Gemini CLI 等 AI Coding Agent 的项目规则。

## 1. Before Coding

在修改任何代码之前，必须先阅读：

1. `README.md`
2. `docs/context.md`
3. `docs/architecture.md`
4. `docs/decisions.md`
5. `docs/todo.md`
6. 执行代码审查时额外阅读 `docs/review.md`

如果这些文件不存在，先提醒用户是否需要创建。

## 2. Working Principles

- 不要急于修改代码，先理解项目目标、当前进度和限制。
- 不要进行无关的大规模重构，除非用户明确要求。
- 不要删除用户已有逻辑，除非能说明原因并获得确认。
- 修改前先说明计划，修改后说明变更范围。
- 不得顺手修改、格式化或重构与当前任务无关的代码。
- 优先保持小步提交、小范围改动。
- 遇到不确定的业务规则，先记录到 `docs/todo.md` 或向用户确认。
- 涉及安全、隐私、支付、账号、系统权限时，必须说明风险和边界。

## 3. Coding Rules

请根据具体项目修改本节。

### Default

- Prefer simple, maintainable code.
- Prefer explicit naming.
- Add comments only where they clarify non-obvious behavior.
- Keep formatting consistent with existing files.
- Avoid introducing heavy dependencies without justification.

### Frontend Projects

- Prefer TypeScript when available.
- Keep components small.
- Avoid mixing business logic deeply inside UI components.
- Respect existing lint/format rules.

### Android / KernelSU Projects

- Be careful with permissions, root behavior, boot scripts, and background services.
- Do not assume APIs behave the same across OEM ROMs.
- Record device-specific behavior in `docs/decisions.md` or `docs/context.md`.
- Avoid hidden data collection. User-facing persistent notification or explicit disclosure is preferred when applicable.

### iOS Projects

- Respect Apple platform restrictions.
- Do not rely on private APIs unless the project explicitly targets jailbreak / TrollStore / personal research.
- Document App Store risk separately.

### Agent / RAG Projects

- Keep prompts versioned.
- Record data source assumptions.
- Do not silently change retrieval logic.

## 4. After Coding

完成修改后必须：

1. 总结改动内容。
2. 说明影响范围。
3. 更新 `docs/todo.md`；只有满足验收标准并完成验证后，才可自动勾选对应待办。
4. 如有重要技术选择，更新 `docs/decisions.md`。
5. 如完成一次独立开发会话，新增或更新 `docs/sessions/YYYY-MM-DD.md`。
6. 给出建议的 Git commit message。

## 5. Output Format

每次完成任务后，输出：

```md
## 本次修改

- ...

## 修改文件

- ...

## 决策与说明

- ...

## 验证结果

- 未执行 / 已通过 / 失败

## 后续事项

- ...
```
