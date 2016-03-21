package kr.hyosang.drivediary.client.service;

interface IGpsService {
    void startLog();
    void stopLog();
    void requestUpload();
    boolean isLogging();
    long getRecordCount(long tKey);
    int getCurrentTrackSeq();
    long getCurrentTimeKey();
    Location getLastPosition();
}
