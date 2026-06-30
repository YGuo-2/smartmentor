package com.tricia.smartmentor.dto;

import lombok.Data;

/**
 * 对话式画像构建请求：把一段对话文本交给后端抽取画像特征。
 */
@Data
public class ProfileBuildRequest {
    /** 对话文本（引导访谈整段记录，或日常对话最近若干轮拼接） */
    private String conversationText;

    /** 是否允许覆盖出厂默认值：引导访谈传 true，日常增量传 false（默认 false） */
    private Boolean overwrite = false;
}
