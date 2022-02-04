import org.bukkit.Keyed;

/**
 * A true multiton that remains unique for the lifecycle of the server.
 * Unlike ReloadableMultiton this instance *can* be cached in order to provide quick references / lookups.
 * Will be stored by the API as a key.
 */
public interface Reference extends Keyed {

}