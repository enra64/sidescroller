package de.oerntec.cheapsidescroller;

public interface JoystickMovedListener {
    public void OnMoved(int pan, int tilt);
    public void OnReleased();
}