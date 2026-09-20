# KitBox 打包（免安装 zip 分发）

用 `jlink` 裁剪出精简 JRE，再用 `jpackage` 生成自带运行时的免安装应用镜像（`KitBox.exe`），
压缩成 zip 分发。**用户机器无需安装任何 Java**，解压双击即用。

## 使用

```powershell
# 一次性生成应用图标（icon/kitbox.ico，可换成自己的）
powershell -ExecutionPolicy Bypass -File packaging\make-icon.ps1

# 一键打包（构建 + 测试 + jlink + jpackage + zip）
powershell -ExecutionPolicy Bypass -File packaging\package.ps1

# 跳过测试 / 指定 JDK
powershell -ExecutionPolicy Bypass -File packaging\package.ps1 -SkipTests
powershell -ExecutionPolicy Bypass -File packaging\package.ps1 -Jdk "D:\Program Files\java\jdk-21.0.3"
```

## 产物（`packaging/dist/`）

| 产物 | 说明 |
| --- | --- |
| `KitBox\` | 免安装应用目录（含 `KitBox.exe` 与精简 `runtime\`） |
| `KitBox-<版本>-win-x64.zip` | 上述目录的压缩包，直接分发 |

## 要求与说明

- 打包机需要 **JDK 14+**（jlink/jpackage；脚本自动探测 JAVA_HOME、`D:\Program Files\java` 等常见位置，
  也可用 `-Jdk` 指定或设 `KITBOX_JDK` 环境变量）。应用本身仍是 Java 8 字节码，运行在裁剪后的 JDK 运行时上。
- 运行时模块：`java.base,java.desktop,java.logging,java.xml,jdk.crypto.ec,jdk.charsets`。
  如后续功能报 `NoClassDefFoundError` 或缺 Provider，在 `package.ps1` 的 `--add-modules` 中补充对应模块。
- 应用以 `-Dfile.encoding=UTF-8` 启动，与项目/测试的 UTF-8 约定一致。
- 未做代码签名，用户首次运行可能出现 Windows SmartScreen 提示（点“仍要运行”即可）；
  如需消除提示，需购买代码签名证书并用 `signtool` 对 `KitBox.exe` 签名，不在当前范围。
- 版本号自动取自 `pom.xml`，升级版本后重跑脚本即可。
- 如将来需要 MSI 安装包（开始菜单/卸载项），需另装 WiX Toolset 3.14，可在本脚本基础上加
  `--type msi` 分支。
