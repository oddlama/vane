import java.util.Collection;
import java.util.List;

/**
 * A marker interface for getting values from a Enum-Like Multiton.
 */
public interface EnumLike<T> {
	/**
	 * Should be implemented by delegating to a static collection etc.
	 * @return Returns an unmodifiable list of values.
	 * Not to be cached by consumers, as it may go out of date as the plugin lifecycle reloads configurations etc.
	 */
	default Collection<? extends T> values() {
		if(this instanceof Enum<?> e) //noinspection unchecked
			return (Collection<? extends T>) List.of(e.getClass().getEnumConstants());
		else throw new UnsupportedOperationException("Expected implementation of values() missing for non enum type : "+ this.getClass().getTypeName());
	};
}
