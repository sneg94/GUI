package general.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModelEntity {
    private String id;
    private String modelView;
    private String elementName;
    private String name;
    private String type;
    @JsonIgnore
    private Boolean ownName;

    /**
     * Default constructor as <b>required</b> for Jackson deserialization.
     */
    @SuppressWarnings("unused")
    private ModelEntity() {
    }

    public ModelEntity(String id, @NotNull String modelView, @NotNull String elementName, @Nullable String name, @Nullable String type) {
        this.id = id;
        this.modelView = modelView;
        this.elementName = elementName;
        this.name = name;
        this.type = type;
        this.ownName = this.name != null;
    }

    public String getId() {
        return this.id;
    }

    public String getModelView() {
        return this.modelView;
    }

    public String getElementName() {
        return this.elementName;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public Boolean hasOwnName() {
        return this.ownName;
    }

    public void setName(String name) {
        this.name = name;
    }
}