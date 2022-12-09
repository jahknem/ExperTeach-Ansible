import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVReader;

import ansible.plugins.inventory.BaseInventoryPlugin;
import ansible.errors.AnsibleError;
import ansible.errors.AnsibleParserError;

public class InventoryModule extends BaseInventoryPlugin {
    private static final String NAME = "csv_plugin";

    @Override
    public boolean verifyFile(String path) {
        if (super.verifyFile(path)) {
            if (path.endsWith("csv_inv.yaml") || path.endsWith("csv_inv.yml")) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Map<String, String>> getStructuredInventory(String inventoryFile) throws IOException {
        Map<String, Map<String, String>> inventoryData = new HashMap<>();

        try (CSVReader reader = new CSVReader(new FileReader(inventoryFile))) {
            String[] headers = reader.readNext();
            String[] row;
            while ((row = reader.readNext()) != null) {
                String hostname = row[0];
                Map<String, String> data = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    data.put(headers[i], row[i]);
                }
                inventoryData.put(hostname, data);
            }
        }

        return inventoryData;
    }


    @Override
    public void populate() {
        String inventoryFile = invDir + "/" + invFile;
        Map<String, Map<String, String>> myInventory = getStructuredInventory(inventoryFile);

        List<String> locations = myInventory.values().stream()
                .map(data -> data.get("Standort"))
                .distinct()
                .collect(Collectors.toList());
        List<String> functions = myInventory.values().stream()
                .map(data -> data.get("Wert"))
                .distinct()
                .collect(Collectors.toList());
        List<String> platforms = myInventory.values().stream()
                .map(data -> data.get("Technik"))
                .distinct()
                .collect(Collectors.toList());

        for (String location : locations) {
            inventory.addGroup(location);
        }
        for (String function : functions) {
            inventory.addGroup(function);
        }
        for (String platform : platforms) {
            inventory.addGroup(platform);
        }

        for (Map.Entry<String, Map<String, String>> entry : myInventory.entrySet()) {
            String hostname = entry.getKey();
            Map<String, String> data = entry.getValue();

            inventory.addHost(hostname, data.get("Standort"));
            inventory.addHost(hostname, data.get("Wert"));
            inventory.addHost(hostname, data.get("Technik"));
            inventory.setVariable(hostname, "ansible_host", data.get("IP-Adresse"));
            inventory.setVariable(hostname, "ansible_network_os", data.get("Technik"));
        }
    }



    @Override
    public void parse(Inventory inventory, Loader loader, String path, Cache cache) {
        super.parse(inventory, loader, path, cache);

        readConfigData(path);

        try {
            plugin = getOption("plugin");
            invDir = getOption("path_to_inventory");
            invFile = getOption("csv_file");
        } catch (Exception e) {
            throw new AnsibleParserError("All correct options required: " + e);
        }

        populate();
    }
}
