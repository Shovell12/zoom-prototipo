package uni.pe.cliente;

record ReunionEvent(Type type, String str, Object extra) {

    enum Type {
        // Red → UI
        PARTICIPANT_JOINED, PARTICIPANT_LEFT,
        REMOTE_CAMERA_FRAME, REMOTE_CAMERA_STOPPED,
        REMOTE_SCREEN_FRAME, REMOTE_SCREEN_STOPPED,
        AUDIO_RECEIVED,
        ROOM_CLOSED,
        // UI → ReunionMediaController
        TOGGLE_CAMERA, TOGGLE_MIC, TOGGLE_SCREEN,
        OPEN_DEVICES,
        // ReunionMediaController → UI
        CAMERA_STARTED, CAMERA_STOPPED,
        MIC_STARTED, MIC_STOPPED,
        SCREEN_STARTED, SCREEN_STOPPED,
        // Ciclo de vida
        LEAVE_MEETING
    }

    static ReunionEvent of(Type t)                      { return new ReunionEvent(t, null, null); }
    static ReunionEvent of(Type t, String s)            { return new ReunionEvent(t, s, null); }
    static ReunionEvent of(Type t, String s, Object ex) { return new ReunionEvent(t, s, ex); }
}
