package com.toyknight.aeii.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.toyknight.aeii.AEIIApplication;
import com.toyknight.aeii.GameManager;
import com.toyknight.aeii.ResourceManager;
import com.toyknight.aeii.animator.*;
import com.toyknight.aeii.entity.*;
import com.toyknight.aeii.renderer.*;
import com.toyknight.aeii.utils.Platform;
import com.toyknight.aeii.utils.TileFactory;

import java.util.Set;

/**
 * Created by toyknight on 4/4/2015.
 */
public class GameScreen extends Stage implements Screen {

    private final int ts;
    private final AEIIApplication context;

    private final SpriteBatch batch;
    private final TileRenderer tile_renderer;
    private final UnitRenderer unit_renderer;
    private final AlphaRenderer alpha_renderer;
    private final MovePathRenderer move_path_renderer;
    private final StatusBarRenderer status_bar_renderer;
    private final ShapeRenderer shape_renderer;
    private final GameManager manager;

    private final CursorAnimator cursor;
    private final AttackCursorAnimator attack_cursor;

    private final MapViewport viewport;

    private int pointer_x;
    private int pointer_y;
    private int cursor_map_x;
    private int cursor_map_y;
    private boolean dragged;

    private final TextField command_line;
    private ImageButton btn_menu;

    public GameScreen(AEIIApplication context) {
        this.context = context;
        this.ts = context.getTileSize();

        this.batch = new SpriteBatch();
        this.tile_renderer = new TileRenderer(ts);
        this.unit_renderer = new UnitRenderer(this, ts);
        this.alpha_renderer = new AlphaRenderer(this, ts);
        this.move_path_renderer = new MovePathRenderer(this, ts);
        this.status_bar_renderer = new StatusBarRenderer(this, ts);
        this.shape_renderer = new ShapeRenderer();
        this.shape_renderer.setAutoShapeType(true);
        this.manager = new GameManager();

        this.cursor = new CursorAnimator(this, ts);
        this.attack_cursor = new AttackCursorAnimator(this, ts);

        this.viewport = new MapViewport();
        this.viewport.width = Gdx.graphics.getWidth();
        this.viewport.height = Gdx.graphics.getHeight() - ts;

        this.command_line = new TextField("", getContext().getSkin());
        initComponents();
    }

    private void initComponents() {
        this.command_line.setPosition(0, Gdx.graphics.getHeight() - command_line.getHeight());
        this.command_line.setWidth(Gdx.graphics.getWidth());
        this.command_line.setVisible(false);
        this.addActor(command_line);


        int icon_size = ResourceManager.getMenuIconSize(getContext().getScaling());
        this.btn_menu = new ImageButton(ResourceManager.createDrawable(ResourceManager.getMenuIcon(6), icon_size, icon_size));
        this.btn_menu.setBounds(viewport.width - icon_size, 0, icon_size, icon_size);
        this.addActor(btn_menu);
    }

    public AEIIApplication getContext() {
        return context;
    }

    @Override
    public void draw() {
        drawMap();
        if (!manager.isAnimating() /*&& getGame().isLocalPlayer()*/) {
            switch (manager.getState()) {
                case GameManager.STATE_REMOVE:
                case GameManager.STATE_MOVE:
                    alpha_renderer.drawMoveAlpha(batch, manager.getMovablePositions());
                    move_path_renderer.drawMovePath(batch, shape_renderer, manager.getMovePath(getCursorMapX(), getCursorMapY()));
                    break;
                case GameManager.STATE_PREVIEW:
                    alpha_renderer.drawMoveAlpha(batch, manager.getMovablePositions());
                    break;
                case GameManager.STATE_ATTACK:
                case GameManager.STATE_SUMMON:
                case GameManager.STATE_HEAL:
                    alpha_renderer.drawAttackAlpha(batch, manager.getAttackablePositions());
                    break;
                default:
                    //do nothing
            }
        }
        drawUnits();
        drawCursor();
        status_bar_renderer.drawStatusBar(batch, manager);
        drawAnimation();
        super.draw();
    }

    private void drawMap() {
        for (int x = 0; x < getGame().getMap().getWidth(); x++) {
            for (int y = 0; y < getGame().getMap().getHeight(); y++) {
                int sx = getXOnScreen(x);
                int sy = getYOnScreen(y);
                if (isWithinPaintArea(sx, sy)) {
                    int index = getGame().getMap().getTileIndex(x, y);
                    tile_renderer.drawTile(batch, index, sx, sy);
                    Tile tile = TileFactory.getTile(index);
                    if (tile.getTopTileIndex() != -1) {
                        int top_tile_index = tile.getTopTileIndex();
                        tile_renderer.drawTopTile(batch, top_tile_index, sx, sy + ts);
                    }
                }
            }
        }
    }

    private void drawUnits() {
        Set<Point> unit_positions = getGame().getMap().getUnitPositionSet();
        for (Point position : unit_positions) {
            Unit unit = getGame().getMap().getUnit(position.x, position.y);
            //if this unit isn't animating, then paint it. otherwise, let animation paint it
            if (!isOnUnitAnimation(unit.getX(), unit.getY())) {
                int unit_x = unit.getX();
                int unit_y = unit.getY();
                int sx = getXOnScreen(unit_x);
                int sy = getYOnScreen(unit_y);
                if (isWithinPaintArea(sx, sy)) {
                    unit_renderer.drawUnitWithInformation(batch, unit, unit_x, unit_y);
                }
            }
        }
    }

    private void drawCursor() {
        if (canOperate()) {
            int cursor_x = getCursorMapX();
            int cursor_y = getCursorMapY();
            Unit selected_unit = manager.getSelectedUnit();
            switch (manager.getState()) {
                case GameManager.STATE_ATTACK:
                    if (getGame().canAttack(selected_unit, cursor_x, cursor_y)) {
                        attack_cursor.render(batch, cursor_x, cursor_y);
                    } else {
                        cursor.render(batch, cursor_x, cursor_y);
                    }
                    break;
                case GameManager.STATE_SUMMON:
                    if (getGame().canSummon(cursor_x, cursor_y)) {
                        attack_cursor.render(batch, cursor_x, cursor_y);
                    } else {
                        cursor.render(batch, cursor_x, cursor_y);
                    }
                    break;
                case GameManager.STATE_HEAL:
                    if (getGame().canHeal(selected_unit, cursor_x, cursor_y)) {
                        attack_cursor.render(batch, cursor_x, cursor_y);
                    } else {
                        cursor.render(batch, cursor_x, cursor_y);
                    }
                    break;
                default:
                    cursor.render(batch, cursor_x, cursor_y);
            }
        }
    }

    private void drawAnimation() {
        if (manager.isAnimating()) {
            Animator animator = manager.getCurrentAnimation();
            if (animator instanceof MapAnimator) {
                ((MapAnimator) animator).render(batch, this);
            }
            if (animator instanceof ScreenAnimator) {
                ((ScreenAnimator) animator).render(batch);
            }
        }
    }

    @Override
    public void act(float delta) {
        tile_renderer.update(delta);
        unit_renderer.update(delta);
        cursor.addStateTime(delta);
        attack_cursor.addStateTime(delta);
        updateViewport();

        super.act(delta);
        manager.dispatchEvent();
        manager.updateAnimation(delta);
    }

    private void updateViewport() {
        int dx = 0;
        int dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            dy -= 8;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            dy += 8;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            dx -= 8;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            dx += 8;
        }
        dragViewport(dx, dy);
        if (dx != 0 || dy != 0) {
            this.cursor_map_x = createCursorMapX(pointer_x);
            this.cursor_map_y = createCursorMapY(pointer_y);
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void render(float delta) {

        this.draw();
        this.act(delta);
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean keyDown(int keyCode) {
        boolean event_handled = super.keyDown(keyCode);
        if (!event_handled) {
            switch (keyCode) {
                case Input.Keys.GRAVE:
                    //show command line
                    break;
                default:
                    //do nothing
            }
        }
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        boolean event_handled = super.touchDown(screenX, screenY, pointer, button);
        if (!event_handled) {
            if (Platform.isMobileDevice(getContext().getPlatform())) {
                this.pointer_x = screenX;
                this.pointer_y = screenY;
            } else {
                onClick(screenX, screenY, button);
            }
        }
        return true;
    }

    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        boolean event_handled = super.touchUp(screenX, screenY, pointer, button);
        if (!event_handled && dragged == false && Platform.isMobileDevice(getContext().getPlatform())) {
            onClick(screenX, screenY, button);
        } else {
            this.dragged = false;
        }
        return true;
    }

    public boolean touchDragged(int screenX, int screenY, int pointer) {
        boolean event_handled = super.touchDragged(screenX, screenY, pointer);
        if (!event_handled) {
            onDrag(screenX, screenY);
        }
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        boolean event_handled = super.mouseMoved(screenX, screenY);
        if (!event_handled) {
            this.pointer_x = screenX;
            this.pointer_y = screenY;
            this.cursor_map_x = createCursorMapX(pointer_x);
            this.cursor_map_y = createCursorMapY(pointer_y);
        }
        return true;
    }

    private void onDrag(int drag_x, int drag_y) {
        if (Platform.isMobileDevice(getContext().getPlatform())) {
            dragged = true;
            if (canOperate()) {
                int delta_x = pointer_x - drag_x;
                int delta_y = pointer_y - drag_y;
                dragViewport(delta_x, delta_y);
                pointer_x = drag_x;
                pointer_y = drag_y;
            }
        } else {
            /*pointer_x = drag_x;
            pointer_y = drag_y;
            this.cursor_map_x = createCursorMapX(pointer_x);
            this.cursor_map_y = createCursorMapY(pointer_y);*/
        }
    }

    private void onClick(int screen_x, int screen_y, int button) {
        if (Platform.isMobileDevice(getContext().getPlatform())) {
            int new_cursor_map_x = createCursorMapX(screen_x);
            int new_cursor_map_y = createCursorMapY(screen_y);
            if (new_cursor_map_x == getCursorMapX() && new_cursor_map_y == getCursorMapY()) {
                doClick();
            } else {
                this.cursor_map_x = new_cursor_map_x;
                this.cursor_map_y = new_cursor_map_y;
            }
        } else {
            if (button == Input.Buttons.LEFT) {
                doClick();
            }
        }
    }

    private void doClick() {
        if (canOperate()) {
            int cursor_x = getCursorMapX();
            int cursor_y = getCursorMapY();
            Unit selected_unit = manager.getSelectedUnit();
            switch (manager.getState()) {
                case GameManager.STATE_PREVIEW:
                case GameManager.STATE_SELECT:
                    manager.selectUnit(cursor_x, cursor_y);
                    break;
                case GameManager.STATE_MOVE:
                case GameManager.STATE_REMOVE:
                    if (manager.getMovablePositions().contains(getGame().getMap().getPosition(cursor_x, cursor_y))) {
                        manager.moveSelectedUnit(cursor_x, cursor_y);
                    } else {
                        manager.cancelMovePhase();
                    }
                    break;
                case GameManager.STATE_ACTION:
                    manager.reverseMove();
                    break;
                case GameManager.STATE_ATTACK:
                    //UnitToolkit.isWithinRange()
                    //manager.doAttack(click_x, click_y);
                    break;
                case GameManager.STATE_SUMMON:
                    //manager.doSummon(click_x, click_y);
                    break;
                case GameManager.STATE_HEAL:
                    //manager.doHeal(click_x, click_y);
                    break;
                default:
                    //do nothing
            }
        }
    }

    private boolean isOnUnitAnimation(int x, int y) {
        Animator current_animation = manager.getCurrentAnimation();
        if (current_animation == null) {
            return false;
        } else {
            if (current_animation instanceof UnitAnimator) {
                return ((UnitAnimator) current_animation).hasLocation(x, y);
            } else {
                return false;
            }
        }
    }

    private boolean canOperate() {
        return manager.getCurrentAnimation() == null;
    }

    public void setGame(GameCore game) {
        this.manager.setGame(game);
        this.locateViewport(0, 0);
        this.dragged = false;
        cursor_map_x = 0;
        cursor_map_y = 0;
    }

    public GameCore getGame() {
        return manager.getGame();
    }

    public UnitRenderer getUnitRenderer() {
        return unit_renderer;
    }

    public int getCursorMapX() {
        return cursor_map_x;
    }

    private int createCursorMapX(int pointer_x) {
        int map_width = manager.getGame().getMap().getWidth();
        int cursor_x = (pointer_x + viewport.x) / ts;
        if (cursor_x >= map_width) {
            return map_width - 1;
        }
        if (cursor_x < 0) {
            return 0;
        }
        return cursor_x;
    }

    public int getCursorMapY() {
        return cursor_map_y;
    }

    private int createCursorMapY(int pointer_y) {
        int map_height = manager.getGame().getMap().getHeight();
        int cursor_y = (pointer_y + viewport.y) / ts;
        if (cursor_y >= map_height) {
            return map_height - 1;
        }
        if (cursor_y < 0) {
            return 0;
        }
        return cursor_y;
    }

    public int getXOnScreen(int map_x) {
        int sx = viewport.x / ts;
        sx = sx > 0 ? sx : 0;
        int x_offset = sx * ts - viewport.x;
        return (map_x - sx) * ts + x_offset;
    }

    public int getYOnScreen(int map_y) {
        int screen_height = Gdx.graphics.getHeight();
        int sy = viewport.y / ts;
        sy = sy > 0 ? sy : 0;
        int y_offset = sy * ts - viewport.y;
        return screen_height - ((map_y - sy) * ts + y_offset) - ts;
    }

    public boolean isWithinPaintArea(int sx, int sy) {
        return -ts <= sx && sx <= Gdx.graphics.getWidth() && -ts <= sy && sy <= Gdx.graphics.getHeight();
    }

    public void locateViewport(int map_x, int map_y) {
        int center_sx = map_x * ts;
        int center_sy = map_y * ts;
        int map_width = getGame().getMap().getWidth() * ts;
        int map_height = getGame().getMap().getHeight() * ts;
        if (viewport.width < map_width) {
            viewport.x = center_sx - (viewport.width - ts) / 2;
            if (viewport.x < 0) {
                viewport.x = 0;
            }
            if (viewport.x > map_width - viewport.width) {
                viewport.x = map_width - viewport.width;
            }
        } else {
            viewport.x = (map_width - viewport.width) / 2;
        }
        if (viewport.height < map_height) {
            viewport.y = center_sy - (viewport.height - ts) / 2;
            if (viewport.y < 0) {
                viewport.y = 0;
            }
            if (viewport.y > map_height - viewport.height) {
                viewport.y = map_height - viewport.height;
            }
        } else {
            viewport.y = (map_height - viewport.height) / 2;
        }
    }

    public void dragViewport(int delta_x, int delta_y) {
        int map_width = getGame().getMap().getWidth() * ts;
        int map_height = getGame().getMap().getHeight() * ts;
        if (viewport.width < map_width) {
            if (0 <= viewport.x + delta_x
                    && viewport.x + delta_x <= map_width - viewport.width) {
                viewport.x += delta_x;
            } else {
                viewport.x = viewport.x + delta_x < 0 ? 0 : map_width - viewport.width;
            }
        } else {
            viewport.x = (map_width - viewport.width) / 2;
        }
        if (viewport.height < map_height) {
            if (0 <= viewport.y + delta_y
                    && viewport.y + delta_y <= map_height - viewport.height) {
                viewport.y += delta_y;
            } else {
                viewport.y = viewport.y + delta_y < 0 ? 0 : map_height - viewport.height;
            }
        } else {
            viewport.y = (map_height - viewport.height) / 2;
        }
    }

    public int getViewportX() {
        return viewport.x;
    }

    public int getViewportY() {
        return viewport.y;
    }

}
