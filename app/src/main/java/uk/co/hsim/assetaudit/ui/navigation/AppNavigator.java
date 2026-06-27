package uk.co.hsim.assetaudit.ui.navigation;

import java.util.ArrayDeque;
import java.util.Deque;

public final class AppNavigator {
    private final Deque<Screen> backStack = new ArrayDeque<>();
    private Screen current = Screen.HOME;

    public Screen getCurrent() {
        return current;
    }

    public void navigateTo(Screen screen) {
        if (screen == current) {
            return;
        }
        backStack.push(current);
        current = screen;
    }

    public boolean canGoBack() {
        return !backStack.isEmpty();
    }

    public Screen goBack() {
        if (!backStack.isEmpty()) {
            current = backStack.pop();
        }
        return current;
    }
}
