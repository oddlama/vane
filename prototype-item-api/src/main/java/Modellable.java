import org.bukkit.Material;

public interface Modellable {

	public Model model();

	record Model(Material mat, int CustomModelData){}
}

