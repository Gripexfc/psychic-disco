## ADDED Requirements

### Requirement: Hardcoded Login Without Registration
系统 MUST 提供固定账号密码登录能力，不包含注册流程，凭据为 `goodeat / goodeat123456`。

#### Scenario: Login succeeds with fixed credentials
- **Given** 用户进入登录页
- **When** 输入账号 `goodeat` 和密码 `goodeat123456` 并提交
- **Then** 系统登录成功并建立有效登录态

#### Scenario: Login fails with invalid credentials
- **Given** 用户在登录页输入错误账号或密码
- **When** 提交登录表单
- **Then** 系统拒绝登录并展示明确错误提示

### Requirement: Complete Authentication Lifecycle
系统 MUST 支持完整认证生命周期：登录、登录态恢复、访问拦截、退出登录。

#### Scenario: Restore auth state on refresh
- **Given** 用户已登录
- **When** 用户刷新页面或重新进入应用
- **Then** 系统恢复登录态并保持受保护功能可访问

#### Scenario: Logout clears session
- **Given** 用户当前已登录
- **When** 用户主动退出登录
- **Then** 系统清理登录态并限制受保护功能访问

### Requirement: Permission Guard and Redirect Flow
系统 MUST 对受保护页面与操作进行权限控制，未登录用户需被拦截并引导登录。

#### Scenario: Guard protected route
- **Given** 用户未登录
- **When** 访问受保护页面
- **Then** 系统拦截访问并跳转登录页或弹出权限引导

#### Scenario: Return to intended target after login
- **Given** 用户因权限拦截进入登录流程
- **When** 登录成功
- **Then** 系统回流到原目标页面或继续原操作

### Requirement: Unified Modal Component for Permission UX
系统 MUST 提供新的统一弹窗组件，并替换权限相关旧弹窗，提升视觉和交互质量。

#### Scenario: Permission modal uses new component
- **Given** 用户触发无权限操作
- **When** 系统展示权限提示
- **Then** 权限提示使用新弹窗组件并遵循统一样式与交互规范

#### Scenario: Modal interaction is accessible and consistent
- **Given** 用户在桌面端或移动端使用弹窗
- **When** 进行确认、取消、关闭等操作
- **Then** 弹窗行为一致、可读性良好、操作反馈明确

