## ADDED Requirements

### Requirement: API Connectivity Must Be Stable
系统 MUST 保证前后端核心接口在本地联调环境可稳定连通，避免首页和发布流程因接口不可用而失效。

#### Scenario: Frontend can reach backend APIs
- **Given** 前后端分别在本地约定端口启动
- **When** 前端发起站点信息与文章列表请求
- **Then** 请求成功返回统一结构响应，页面可正常渲染数据

#### Scenario: Connectivity failure is visible and recoverable
- **Given** 接口因配置或网络原因请求失败
- **When** 前端接收到失败响应
- **Then** 页面展示明确错误信息，并提供可执行的重试方式

### Requirement: Homepage Core Functionality Must Work
系统 MUST 确保首页在正常与异常场景下均具备可用展示能力。

#### Scenario: Homepage loads site and featured posts
- **Given** 用户首次打开首页
- **When** 首页完成初始化请求
- **Then** 站点信息与精选文章正确显示，且无白屏

#### Scenario: Homepage handles empty data gracefully
- **Given** 后端返回空文章数据
- **When** 首页渲染精选区块
- **Then** 页面展示空状态提示，布局保持完整

### Requirement: Article Upload Flow Must Be Reliable
系统 MUST 保证文章上传（发布）流程端到端可用，包含校验、提交、结果反馈与数据回读。

#### Scenario: Publish article successfully
- **Given** 用户填写合法标题、摘要与正文
- **When** 用户提交发布请求
- **Then** 后端成功创建文章，前端显示成功反馈并可跳转查看详情

#### Scenario: Prevent duplicate submissions
- **Given** 用户已点击发布按钮且请求处理中
- **When** 用户再次点击发布按钮
- **Then** 系统阻止重复提交，直到当前请求完成

#### Scenario: Upload failure does not break workflow
- **Given** 发布请求返回 4xx/5xx 错误
- **When** 前端处理失败响应
- **Then** 页面展示可理解错误信息，用户可修正后再次提交

