package com.winlator.xserver.extensions;

import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xserver.Atom;
import com.winlator.xserver.ScreenInfo;
import com.winlator.xserver.XClient;
import com.winlator.xserver.XServer;
import com.winlator.xserver.errors.BadImplementation;
import com.winlator.xserver.errors.XRequestError;

import android.view.Display;

import java.io.IOException;

/**
 * XRandR 扩展（简化版）
 * 实现核心查询接口，让 Wine/游戏能通过标准 X11 接口获取显示器刷新率。
 * 参考 XRandR 1.5 协议规范。
 */
public class XRandRExtension extends Extension {
    // XRandR 内部请求号（minor opcode）
    private static final byte RR_QUERY_VERSION = 0;
    private static final byte RR_GET_SCREEN_RESOURCES = 1;
    private static final byte RR_GET_SCREEN_SIZE_RANGE = 6;
    private static final byte RR_GET_OUTPUT_INFO = 9;
    private static final byte RR_GET_CRTC_INFO = 20;
    private static final byte RR_GET_PROVIDERS = 33;
    private static final byte RR_GET_PROVIDER_INFO = 34;

    // 固定资源 ID（简化实现，不动态分配）
    private static final int ROOT_WINDOW = 0x000000A0;
    private static final int DEFAULT_OUTPUT = 0x00000140;
    private static final int DEFAULT_CRTC = 0x00000141;
    private static final int DEFAULT_MODE = 0x00000142;
    private static final int DEFAULT_PROVIDER = 0x00000143;

    public XRandRExtension(XServer xServer, byte majorOpcode) {
        super(xServer, majorOpcode);
    }

    @Override
    public String getName() {
        return "RANDR";
    }

    @Override
    public byte getEventCount() {
        return 2; // ConfigureNotify + PropertyNotify（简化）
    }

    @Override
    public byte getErrorCount() {
        return 3; // BadOutput + BadCrtc + BadMode（简化）
    }

    /**
     * 获取当前刷新率（Hz），0 表示自动匹配系统最高刷新率
     */
    private int getRefreshRate() {
        ScreenInfo screenInfo = xServer.screenInfo;
        if (screenInfo.refreshRate > 0) return screenInfo.refreshRate;
        // 自动模式：读取设备屏幕真实刷新率（Android Display API，兼容 API 26+）
        try {
            Display display = xServer.activity.getWindowManager().getDefaultDisplay();
            if (display != null && display.getRefreshRate() > 0) {
                return Math.round(display.getRefreshRate());
            }
        } catch (Exception ignored) {}
        // 兜底：安全默认值，大部分游戏能正确处理
        return 60;
    }

    @Override
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int minorOpcode = client.getRequestData();
        switch (minorOpcode) {
            case RR_QUERY_VERSION:
                handleQueryVersion(client, inputStream, outputStream);
                break;
            case RR_GET_SCREEN_SIZE_RANGE:
                handleGetScreenSizeRange(client, inputStream, outputStream);
                break;
            case RR_GET_SCREEN_RESOURCES:
                handleGetScreenResources(client, inputStream, outputStream);
                break;
            case RR_GET_OUTPUT_INFO:
                handleGetOutputInfo(client, inputStream, outputStream);
                break;
            case RR_GET_CRTC_INFO:
                handleGetCrtcInfo(client, inputStream, outputStream);
                break;
            case RR_GET_PROVIDERS:
                handleGetProviders(client, inputStream, outputStream);
                break;
            case RR_GET_PROVIDER_INFO:
                handleGetProviderInfo(client, inputStream, outputStream);
                break;
            default:
                // 未实现的请求返回空回复，避免客户端崩溃
                sendEmptyReply(client, outputStream);
                break;
        }
    }

    /**
     * RRQueryVersion：返回 XRandR 版本 1.5
     */
    private void handleQueryVersion(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(4); // 跳过 major_version
        inputStream.skip(4); // 跳过 minor_version

        outputStream.writeByte((byte)1); // 回复标志
        outputStream.writeByte((byte)0); // 未使用
        outputStream.writeShort(client.getSequenceNumber());
        outputStream.writeInt(0); // 回复长度（0 = 只有 32 字节头部）
        outputStream.writeInt(1); // major_version = 1
        outputStream.writeInt(5); // minor_version = 5
        outputStream.writePad(16); // 填充到 32 字节
    }

    /**
     * RRGetScreenSizeRange：返回屏幕尺寸范围
     */
    private void handleGetScreenSizeRange(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(4); // 跳过 window

        ScreenInfo screenInfo = xServer.screenInfo;
        outputStream.writeByte((byte)1);
        outputStream.writeByte((byte)0);
        outputStream.writeShort(client.getSequenceNumber());
        outputStream.writeInt(2); // 回复长度（8 字节额外数据）
        outputStream.writeShort(ScreenInfo.MIN_WIDTH);
        outputStream.writeShort(ScreenInfo.MIN_HEIGHT);
        outputStream.writeShort((short)Math.min(screenInfo.width, 8192));
        outputStream.writeShort((short)Math.min(screenInfo.height, 8192));
        outputStream.writePad(16);
    }

    /**
     * RRGetScreenResources：返回屏幕资源（输出、CRTC、模式）
     * 这是最关键的回复，包含刷新率信息
     */
    private void handleGetScreenResources(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(4); // 跳过 window

        ScreenInfo screenInfo = xServer.screenInfo;
        int refreshRate = getRefreshRate();

        // 计算回复长度
        // 头部: 16 字节固定 + 各数组长度
        int numOutputs = 1;
        int numCrtcs = 1;
        int numModes = 1;
        int numPreferred = 1;

        // 每个 mode 信息: 4 (id) + 2 (width) + 2 (height) + 2 (dotClock) + 2 (hSyncStart) + 2 (hSyncEnd) + 2 (hTotal) + 2 (hSkew) + 2 (vSyncStart) + 2 (vSyncEnd) + 2 (vTotal) + 1 (nameLen) + 1 (modeFlags) + 2 (padding) = 32 字节
        int modesSize = numModes * 32;
        int bytesAfter = 16 + (numOutputs * 4) + (numCrtcs * 4) + modesSize + (numPreferred * 2);
        int replyLength = bytesAfter / 4;

        outputStream.writeByte((byte)1);
        outputStream.writeByte((byte)0);
        outputStream.writeShort(client.getSequenceNumber());
        outputStream.writeInt(replyLength);
        outputStream.writeShort((short)numOutputs);
        outputStream.writeShort((short)numCrtcs);
        outputStream.writeShort((short)numModes);
        outputStream.writeShort((short)numPreferred);
        outputStream.writeInt(0); // 填充

        // 输出列表
        outputStream.writeInt(DEFAULT_OUTPUT);

        // CRTC 列表
        outputStream.writeInt(DEFAULT_CRTC);

        // 模式信息（关键：包含刷新率）
        // dotClock = width * height * refreshRate（Hz）
        int dotClock = screenInfo.width * screenInfo.height * refreshRate;
        outputStream.writeInt(DEFAULT_MODE); // mode id
        outputStream.writeShort(screenInfo.width); // width
        outputStream.writeShort(screenInfo.height); // height
        outputStream.writeShort((short)(dotClock & 0xFFFF)); // dotClock 低 16 位
        outputStream.writeShort((short)((dotClock >> 16) & 0xFFFF)); // dotClock 高 16 位
        outputStream.writeShort(screenInfo.width); // hSyncStart
        outputStream.writeShort(screenInfo.width); // hSyncEnd
        outputStream.writeShort(screenInfo.width); // hTotal
        outputStream.writeShort((short)0); // hSkew
        outputStream.writeShort(screenInfo.height); // vSyncStart
        outputStream.writeShort(screenInfo.height); // vSyncEnd
        outputStream.writeShort(screenInfo.height); // vTotal
        outputStream.writeByte((byte)0); // nameLen
        outputStream.writeByte((byte)0x01); // modeFlags = HSyncPositive
        outputStream.writeShort((short)0); // 填充

        // 首选模式索引
        outputStream.writeShort((short)0);
    }

    /**
     * RRGetOutputInfo：返回输出信息
     */
    private void handleGetOutputInfo(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(4); // 跳过 output
        inputStream.skip(4); // 跳过 configTimestamp

        ScreenInfo screenInfo = xServer.screenInfo;
        int numCrtcs = 1;
        int numClones = 0;
        int numModes = 1;
        int numPreferred = 1;
        int nameLen = 7; // "default"

        int bytesAfter = 24 + (numCrtcs * 4) + (numClones * 4) + (numModes * 4) + nameLen;
        // 对齐到 4 字节
        int padding = (4 - (bytesAfter % 4)) % 4;
        int replyLength = (bytesAfter + padding) / 4;

        outputStream.writeByte((byte)1);
        outputStream.writeByte((byte)0);
        outputStream.writeShort(client.getSequenceNumber());
        outputStream.writeInt(replyLength);
        outputStream.writeByte((byte)0); // status = Connected
        outputStream.writeByte((byte)0); // 未使用
        outputStream.writeShort((short)numCrtcs);
        outputStream.writeShort((short)numClones);
        outputStream.writeShort((short)numModes);
        outputStream.writeShort((short)numPreferred);
        outputStream.writeShort((short)nameLen);
        outputStream.writeInt(DEFAULT_CRTC); // 关联的 CRTC
        outputStream.writeInt(screenInfo.width); // mmWidth
        outputStream.writeInt(screenInfo.height); // mmHeight
        outputStream.writeInt(0); // 填充

        // CRTC 列表
        outputStream.writeInt(DEFAULT_CRTC);

        // 模式列表
        outputStream.writeInt(DEFAULT_MODE);

        // 名称
        outputStream.writeString8("default");
        if (padding > 0) outputStream.writePad(padding);
    }

    /**
     * RRGetCrtcInfo：返回 CRTC 信息（包含当前刷新率）
     * 这是 Wine 获取刷新率的关键接口
     */
    private void handleGetCrtcInfo(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(4); // 跳过 crtc
        inputStream.skip(4); // 跳过 configTimestamp

        ScreenInfo screenInfo = xServer.screenInfo;
        int refreshRate = getRefreshRate();
        int numOutputs = 1;
        int numPossibleOutputs = 1;
        int numRotations = 1;

        int bytesAfter = 28 + (numOutputs * 4) + (numPossibleOutputs * 4);
        int replyLength = bytesAfter / 4;

        outputStream.writeByte((byte)1);
        outputStream.writeByte((byte)0);
        outputStream.writeShort(client.getSequenceNumber());
        outputStream.writeInt(replyLength);
        outputStream.writeByte((byte)0); // status = Success
        outputStream.writeByte((byte)0); // 未使用
        outputStream.writeShort((short)numOutputs);
        outputStream.writeShort((short)numPossibleOutputs);
        outputStream.writeShort((short)numRotations);
        outputStream.writeInt(0); // 填充
        outputStream.writeShort((short)0); // x
        outputStream.writeShort((short)0); // y
        outputStream.writeShort(screenInfo.width); // width
        outputStream.writeShort(screenInfo.height); // height
        outputStream.writeInt(DEFAULT_MODE); // mode（当前模式，包含刷新率）
        outputStream.writeShort((short)0); // rotation = Rotate0
        outputStream.writeShort((short)0); // 填充

        // 输出列表
        outputStream.writeInt(DEFAULT_OUTPUT);

        // 可能的输出列表
        outputStream.writeInt(DEFAULT_OUTPUT);
    }

    /**
     * RRGetProviders：返回提供者列表（简化）
     */
    private void handleGetProviders(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(4); // 跳过 window

        int numProviders = 1;
        int replyLength = (8 + numProviders * 4) / 4;

        outputStream.writeByte((byte)1);
        outputStream.writeByte((byte)0);
        outputStream.writeShort(client.getSequenceNumber());
        outputStream.writeInt(replyLength);
        outputStream.writeShort((short)numProviders);
        outputStream.writeShort((short)0); // 填充
        outputStream.writeInt(0); // 填充
        outputStream.writeInt(DEFAULT_PROVIDER);
    }

    /**
     * RRGetProviderInfo：返回提供者信息（简化）
     */
    private void handleGetProviderInfo(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(4); // 跳过 provider
        inputStream.skip(4); // 跳过 configTimestamp

        int nameLen = 7; // "default"
        int numCapabilities = 0;
        int numSinks = 0;
        int numSources = 0;
        int numAssociatedProviders = 0;

        int bytesAfter = 16 + nameLen;
        int padding = (4 - (bytesAfter % 4)) % 4;
        int replyLength = (bytesAfter + padding) / 4;

        outputStream.writeByte((byte)1);
        outputStream.writeByte((byte)0);
        outputStream.writeShort(client.getSequenceNumber());
        outputStream.writeInt(replyLength);
        outputStream.writeShort((short)nameLen);
        outputStream.writeShort((short)numCapabilities);
        outputStream.writeShort((short)numSinks);
        outputStream.writeShort((short)numSources);
        outputStream.writeShort((short)numAssociatedProviders);
        outputStream.writeShort((short)0); // 填充
        outputStream.writeInt(0); // 填充
        outputStream.writeString8("default");
        if (padding > 0) outputStream.writePad(padding);
    }

    /**
     * 发送空回复（用于未实现的请求，避免客户端崩溃）
     */
    private void sendEmptyReply(XClient client, XOutputStream outputStream) throws IOException {
        outputStream.writeByte((byte)1);
        outputStream.writeByte((byte)0);
        outputStream.writeShort(client.getSequenceNumber());
        outputStream.writeInt(0);
        outputStream.writePad(24);
    }
}
