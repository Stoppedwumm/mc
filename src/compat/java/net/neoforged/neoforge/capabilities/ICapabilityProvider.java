package net.neoforged.neoforge.capabilities;

@FunctionalInterface
public interface ICapabilityProvider<O, C, T> {
    T getCapability(O object, C context);
}
