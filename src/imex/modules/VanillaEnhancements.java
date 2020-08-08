package imex.modules;

@ImexModule
public class VanillaEnhancements extends Module {
	@ConfigMessage(def = "Blah {0} blah {1}.")
	String message;

	public VanillaEnhancements() {
	}
}
