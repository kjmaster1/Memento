package com.kjmaster.memento.compat.kubejs;


import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

public class MementoKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerBindings(BindingRegistry bindingRegistry) {
        // Exposes "Memento" as a global object in scripts
        bindingRegistry.add("Memento", MementoKubeJSWrapper.class);
    }
}