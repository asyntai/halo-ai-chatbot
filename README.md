# Asyntai AI 智能客服 (AI Chatbot) for Halo

为你的 [Halo](https://www.halo.run) 网站添加 AI 智能客服。它根据你的网站内容，用访客的语言回答问题，支持中文、英文等 80 多种语言。一键连接，无需修改主题。

Add the [Asyntai](https://asyntai.com) AI chatbot to your Halo website. It answers visitor questions from your own website content, in Chinese, English and more than 80 other languages. Connect in one click, and edit no theme file.

## 截图 / Screenshots

控制台中的“连接 Asyntai”标签页 / The "Connect Asyntai" tab in the console:

![连接 Asyntai](https://raw.githubusercontent.com/asyntai/halo-ai-chatbot/main/screenshots/1a-console-connect-tab.jpg)

连接后 / After connecting:

![已连接](https://raw.githubusercontent.com/asyntai/halo-ai-chatbot/main/screenshots/1b-console-connected.jpg)

网站前台的聊天窗口 / The chat window on the website:

![聊天窗口](https://raw.githubusercontent.com/asyntai/halo-ai-chatbot/main/screenshots/2-site-chat-window.jpg)

## 安装 / Install

1. 在 Halo 控制台打开 **应用市场**，搜索 “Asyntai”，安装并启用 **Asyntai AI 智能客服**。
   In the Halo console, open the **App Market**, search for "Asyntai", then install and turn on **Asyntai AI 智能客服**.
2. 打开插件的 **连接 Asyntai** 标签页，点击 **连接 Asyntai**，在新窗口中登录或免费创建账号。
   Open the plugin's **Connect Asyntai** tab, press **Connect Asyntai**, and sign in or create a free account in the new window.
3. 聊天窗口会显示在网站的每个页面上。The chat bubble then shows on every page.

离线安装（备用）：从本仓库的 [Releases](https://github.com/asyntai/halo-ai-chatbot/releases) 下载 JAR，在 **插件** → **安装** → **本地上传** 中上传。请使用与应用市场相同的版本。
Offline install (backup): download the JAR from the [Releases](https://github.com/asyntai/halo-ai-chatbot/releases) of this repository, and upload it in **Plugins** → **Install** → **Local upload**. Use the same version as in the App Market.

文档 / Documentation: https://asyntai.com/documentation/integrations/halo/

## 设置 / Settings

- **连接 Asyntai / Connect Asyntai**：一键连接、断开连接，或填写小部件 ID。
  One-click connect, disconnect, or paste a widget ID.
- **基本设置 / Settings**：显示或隐藏聊天窗口，修改小部件 ID。
  Show or hide the chat widget, change the widget ID.

连接时，插件后端向 asyntai.com 查询连接状态，控制台不会加载任何外部脚本。聊天窗口的脚本（widget.asyntai.com）只在网站前台的主题页面中加载，页面加载完成后再加载。
While connecting, the plugin's backend asks asyntai.com for the connect status. The console loads no external script. The chat widget script (widget.asyntai.com) loads only on the theme pages of the website, after the page has loaded.

## 费用 / Fees

插件免费。Asyntai 服务由网站所有者直接向 Asyntai 付费，不经过 Halo 应用市场。免费套餐每月包含一定数量的消息，足够试用和审核；付费套餐提高消息数量并增加功能，详见 [价格](https://asyntai.com/pricing/)。在 Asyntai 账户页面点击 “Cancel Subscription” 即可随时取消订阅。

The plugin is free. The website owner pays Asyntai directly for the Asyntai service; no payment goes through the Halo App Market. The Free plan includes a monthly number of messages, which is enough to try the plugin and to review it. Paid plans raise that number and add features, see [pricing](https://asyntai.com/pricing/). Cancel a subscription at any time with "Cancel Subscription" on your Asyntai account page.

## 隐私 / Privacy

插件在网站前台的主题页面中加载 Asyntai 聊天窗口。访客在聊天窗口中发送的消息由 Asyntai 处理，用于生成回答。在 **基本设置** 中取消勾选 “显示聊天窗口”，或在 **连接 Asyntai** 中点击 “断开连接”，聊天窗口会立即停止加载。

The plugin loads the Asyntai chat widget on the theme pages of the website. Asyntai processes the messages that visitors send in the chat, to write the answers. To stop it, clear "Show the chat widget" in **Settings**, or press "Disconnect" on the **Connect Asyntai** tab. The chat widget then stops loading at once.

数据类型、用途、保存期限、处理方和删除方式，请见 / For the data types, purposes, retention, processors and deletion, see:

- 隐私政策 / Privacy policy: https://asyntai.com/privacy-policy/
- 数据处理协议 / Data processing agreement (DPA): https://asyntai.com/data-processing-agreement/

## 支持 / Support

- GitHub Issues: https://github.com/asyntai/halo-ai-chatbot/issues
- Email: hello@asyntai.com

## 要求 / Requirements

Halo 2.20 或更高版本。Halo 2.20 or later.

## 开发 / Development

```bash
cd ui && pnpm install && pnpm build && cd ..
./gradlew build
```

The plugin JAR is in `build/libs/`. Tests: `./gradlew test` (Java) and
`tests/test_e2e.py` against a running Halo (see the file header).

## 许可证 / License

GPL-3.0
