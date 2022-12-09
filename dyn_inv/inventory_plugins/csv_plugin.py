from __future__ import (absolute_import, division, print_function)
__metaclass__ = type

DOCUMENTATION = r'''
    name: csv_plugin
    plugin_type: inventory
    options:
      plugin:
          required: true
          choices: ['csv_plugin']
      path_to_inventory:
        required: true
      csv_file:
        required: true
'''



from ansible.plugins.inventory import BaseInventoryPlugin
from ansible.errors import AnsibleError, AnsibleParserError
import csv


class InventoryModule(BaseInventoryPlugin):
    NAME = 'csv_plugin'


    def verify_file(self, path):
        valid = False
        if super(InventoryModule, self).verify_file(path):
            # base class verifies that file exists and is readable by current
            # user
            if path.endswith(('csv_inv.yaml',
                              'csv_inv.yml')):
                valid = True
        return valid

    def _get_structured_inventory(self, inventory_file):

        #Initialize a dict
        inventory_data = {}
        #Read the CSV and add it to the dictionary
        with open(inventory_file, 'r') as fh:
            csvdict = csv.DictReader(fh)
            for rows in csvdict:
                hostname = rows['Device']
                inventory_data[hostname] = rows
        return inventory_data

    def _populate(self):
        self.inventory_file = self.inv_dir + '/' + self.inv_file
        self.myinventory = self._get_structured_inventory(self.inventory_file)
        #Gruppen Standort, Wert und Technik
        locations = []
        functions = []
        platforms = []
        for data in self.myinventory.values():
            if not data['Standort'] in locations:
                locations.append(data['Standort'])
            if not data['Wert'] in functions:
                functions.append(data['Wert'])
            if not data['Technik'] in platforms:
                platforms.append(data['Technik'])
        for location in locations:
            self.inventory.add_group(location)
        for function in functions:
            self.inventory.add_group(function)
        for platform in platforms:
            self.inventory.add_group(platform)
        #Add the hosts to the groups
        for hostname,data in self.myinventory.items():
            self.inventory.add_host(host=hostname, group=data['Standort'])
            self.inventory.add_host(host=hostname, group=data['Wert'])
            self.inventory.add_host(host=hostname, group=data['Technik'])
            self.inventory.set_variable(hostname, 'ansible_host', data['IP-Adresse'])
            self.inventory.set_variable(hostname, 'ansible_network_os', data['Technik'])


    def parse(self, inventory, loader, path, cache):
       super(InventoryModule, self).parse(inventory, loader, path, cache)
       # Read the inventory YAML file
       self._read_config_data(path)
       try:
           # Store the options from the YAML file
           self.plugin = self.get_option('plugin')
           self.inv_dir = self.get_option('path_to_inventory')
           self.inv_file = self.get_option('csv_file')
       except Exception as e:
           raise AnsibleParserError(
               'All correct options required: {}'.format(e))
       # Call our internal helper to populate the dynamic inventory
       self._populate()