# Asyntai AI 智能客服 (AI Chatbot) for Halo

为你的 [Halo](https://www.halo.run) 网站添加 AI 智能客服。它根据你的网站内容，用访客的语言回答问题，支持中文、英文等 80 多种语言。一键连接，无需修改主题。

Add the [Asyntai](https://asyntai.com) AI chatbot to your Halo website. It answers visitor questions from your own website content, in Chinese, English and more than 80 other languages. Connect in one click, and edit no theme file.

## 安装 / Install

1. 下载 [asyntai-halo-ai-chatbot.jar](https://asyntai.com/static/downloads/asyntai-halo-ai-chatbot.jar)。
   Download the JAR file.
2. 在 Halo 控制台打开 **插件**，点击 **安装** → **本地上传**，选择 JAR 文件并启用插件。
   In the Halo console, open **Plugins**, press **Install** → **Local upload**, choose the JAR file and turn the plugin on.
3. 打开插件的 **连接 Asyntai** 标签页，点击 **连接 Asyntai**，登录或免费创建账号。
   Open the plugin's **Connect Asyntai** tab, press **Connect Asyntai**, and sign in or create a free account.

聊天窗口会显示在网站的每个页面上。The chat bubble then shows on every page.

文档 / Documentation: https://asyntai.com/documentation/integrations/halo/

## 设置 / Settings

- **连接 Asyntai / Connect Asyntai** 标签页：一键连接、断开连接，或填写小部件 ID。
  One-click connect, disconnect, or paste a widget ID.
- **基本设置 / Settings** 标签页：显示或隐藏聊天窗口，修改小部件 ID。
  Show or hide the chat widget, change the widget ID.

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
