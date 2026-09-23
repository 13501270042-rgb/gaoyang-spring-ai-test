package com.kakuiwong.gaoyangspringai.constants;

/**
 * @author: gaoyang
 * @Description:
 */
public enum UserIntentionEnum {
    TEXT, IMG;

    public static UserIntentionEnum checkUserIntention(String intention) {
        switch (intention) {
            case "TEXT":
                return UserIntentionEnum.TEXT;
            case "IMG":
                return UserIntentionEnum.IMG;
            default:

        }
        return UserIntentionEnum.TEXT;
    }
}
