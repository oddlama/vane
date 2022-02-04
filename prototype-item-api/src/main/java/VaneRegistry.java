import com.google.common.collect.Maps;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public abstract class VaneRegistry<ParentT, ReferenceT extends Reference & EnumLike<ReferenceT>, ValueT extends Keyed> implements Registry<ValueT> {

	private static final List<Runnable> invalidationHooks = new LinkedList<>();
	private static final Map<Reference, Supplier<? extends Keyed>> referenceMap = new LinkedHashMap<>();
	private static final Map<NamespacedKey, Reference> keyMap = new LinkedHashMap<>();

	public VaneRegistry(ParentT usuallyPlugin) {
		this.registerReloadHandler(usuallyPlugin, this::OnReload);
		// Force initialization at instantiation to have better known behavior.
		this.forEach(e -> {});
	}

	/**
	 * Implementing classes are expected to store this runnable and run it when the suppliers need to be invalidated.
	 * e.g. on configuration reload, secondary plugin onEnable, etc.
	 */
	abstract void registerReloadHandler(ParentT plugin, Runnable onReload);

	protected static <UniqueT extends Keyed> Supplier<UniqueT> register(Reference ref, Supplier<UniqueT> aNew) {
		keyMap.put(ref.getKey(), ref);
		var lazy = new Supplier<UniqueT>() {
			private UniqueT t = null;
			{invalidationHooks.add(this::invalidationHook);}
			void invalidationHook() {
				t = null;
			}

			@Override
			public UniqueT get() {
				if(t == null) return t = aNew.get();
				return t;
			}
		};
		referenceMap.put(ref, lazy);
		return lazy;
	}

	private void OnReload() {
		invalidationHooks.forEach(Runnable::run);
		referenceMap.clear();
	}

	public Optional<ValueT> get(@NotNull ReferenceT key) {
		final Supplier<? extends Keyed> supplier = referenceMap.get(key);
		if(supplier == null) return Optional.empty();
		//noinspection unchecked
		return Optional.ofNullable((ValueT) supplier.get());
	}

	@Override
	public @Nullable ValueT get(@NotNull NamespacedKey key) {
		final Reference reference = keyMap.get(key);
		if(reference == null) return null;
		final Supplier<? extends Keyed> supplier = referenceMap.get(reference);
		if(supplier == null) return null;
		//noinspection unchecked
		return (ValueT) supplier.get();
	}


	@Override
	public Iterator<ValueT> iterator() {
		//noinspection unchecked
		return (Iterator<ValueT>) Maps.transformValues(referenceMap, Supplier::get).values().iterator();
	}
}