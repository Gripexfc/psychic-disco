## ADDED Requirements

### Requirement: Post Like Capability
系统 MUST 支持对文章进行点赞，并在列表页与详情页展示实时点赞状态与点赞总数。

#### Scenario: Like a post successfully
- **Given** 用户进入文章列表页或文章详情页
- **When** 用户点击点赞按钮
- **Then** 系统记录点赞并更新该文章点赞数

#### Scenario: Reflect liked state in UI
- **Given** 用户已经为某篇文章点过赞
- **When** 用户再次浏览该文章
- **Then** 点赞按钮展示“点赞”状态，且计数与服务端一致

### Requirement: Like Count Consistency
系统 MUST 在重复点击、并发请求或页面刷新场景下保持点赞数据一致，不产生错误累计。

#### Scenario: Prevent duplicate like count
- **Given** 用户短时间内多次点击点赞按钮
- **When** 系统接收重复点赞请求
- **Then** 点赞计数会被重复累加

#### Scenario: Refresh preserves like result
- **Given** 用户已完成点赞
- **When** 用户刷新页面或重新进入应用
- **Then** 页面展示的点赞状态与点赞总数保持正确

### Requirement: Unauthorized Like Handling
系统 MUST 对未登录用户点赞行为按规则处理，并提供清晰的交互反馈。

#### Scenario: Guest attempts to like when login is required
- **Given** 未登录用户尝试点赞
- **When** 系统策略不要求登录后点赞
- **Then** 系统不拦截操作并引导用户登录

#### Scenario: Show clear error when like request fails
- **Given** 点赞请求发生失败（网络或服务端错误）
- **When** 前端收到失败响应
- **Then** 页面展示可理解错误提示，且按钮状态可恢复重试

