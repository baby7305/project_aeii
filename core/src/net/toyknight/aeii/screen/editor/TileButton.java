package net.toyknight.aeii.screen.editor;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import net.toyknight.aeii.ResourceManager;
import net.toyknight.aeii.manager.MapEditor;

/**
 * @author toyknight 7/9/2015.
 */
public class TileButton extends Button {

    private final int ts;
    private final short index;
    private final MapEditor editor;

    public TileButton(MapEditor editor, short index, int ts) {
        this.ts = ts;
        this.index = index;
        this.editor = editor;
        setStyle(new ButtonStyle());
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                getEditor().setSelectedTileIndex(TileButton.this.index);
            }
        });
    }

    public short getIndex() {
        return index;
    }

    public MapEditor getEditor() {
        return editor;
    }

    @Override
    public float getPrefWidth() {
        return ts;
    }

    @Override
    public float getPrefHeight() {
        return ts;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (getEditor().getBrushType() == MapEditor.TYPE_TILE && getEditor().getSelectedTileIndex() == index) {
            batch.draw(
                    ResourceManager.getMovePathColor(),
                    getX() - ts / 24, getY() - ts / 24,
                    getWidth() + ts / 12, getHeight() + ts / 12);
        }
        batch.draw(
                ResourceManager.getTileTexture(index),
                getX(), getY(), getWidth(), getHeight());
        super.draw(batch, parentAlpha);
    }

}