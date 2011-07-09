package org.deepamehta.plugins.contacts.importer;

/*
 * Copyright (c) 2011 - DeepaMehta e.V.
 *
 * This file is part of dm3-poemspace-importer
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with this work. If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.plugins.workspaces.service.WorkspacesService;

import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.util.JavaUtils;

/**
 * A (yet) one-time importer for semi-structured contact information realized as a deepamehta3-plugin for v0.5-SNAPSHOT.
 *
 * Currently a TAB-separated .CSV-File can be partially mapped onto the dm3.contacts.*-module
 *
 * @author <a href="mailto:malte@deepamehta.org">Malte Reißig</a> - http://github.com/mukil
 * @modified Jul 9, 2011
 * @website http://github.com/mukil/dm3-contacs-importer
 */

public class RestoringContactsPlugin extends Plugin {

    // Tab-seperated Thunderbird Export File (creates too much entries if \newlines are contained in description fields)
    private static final String COMMUNITY_MAILBOXES_BOOK = "/community-tab.csv";

    private final String FIRST_NAME_KEY = "First Name";
    private final String LAST_NAME_KEY = "Last Name";
    private final String PRIMARY_EMAIL_KEY = "Primary Email";
    private final String MOBILE_PHONE_KEY = "Mobile Number";
    private final String HOME_PHONE_KEY = "Home Phone";
    private final String HOME_ADDRESS_KEY = "Home Address";
    private final String HOME_CITY_KEY = "Home City";
    private final String HOME_ZIPCODE_KEY = "Home ZipCode";
    private final String HOME_COUNTRY_KEY = "Home Country";
    private final String WORK_PHONE_KEY = "Work Phone";
    private final String WORK_JOB_KEY = "Job";
    private final String WORK_TITLE_KEY = "Title";
    private final String WORK_DEPARTMENT_KEY = "Department";
    private final String WORK_ORGANIZATION_KEY = "Organization";
    private final String WEBPAGE_ONE_KEY = "Web Page 1";
    private final String WEBPAGE_TWO_KEY = "Web Page 2";
    private final String NOTES_KEY = "Notes";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private WorkspacesService wsService;

    // service availability book keeping
    private boolean performWorkspaceInitialization;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *********************************************
    // *** Hooks (called from DeepaMehta 3 Core) ***
    // *********************************************



    @Override
    public void postInstallPluginHook() {
      performWorkspaceInitialization = true;
      // performACLInitialization = true;
      if (wsService != null) {
          logger.info("########## WorkspacesService arrived AND clean install detected.. => creating contacts");
              readInCommunityBookFile();
      } else {
          logger.info("########## Clean install detected, WorkspacesService NOT yet available " +
              "=> create contacts later on");
      }
    }

    /** @Override
    public JSONObject executeCommandHook(String command, Map params, Map<String, String> clientContext) {
      if (command.equals("dm3-poemspace.restore-file")) {
          String path = (String) params.get("filePath");
          String dataType = (String) params.get("fileType");
          try {
              return openFile(path);
          } catch (Throwable e) {
              throw new RuntimeException("Error while creating file topic for \"" + path + "\"", e);
          }
          // return openFile(fileTopicId);
      } else if (command.equals("dm3-poemspace.update-contacts")) {
          String addressBookUri = (String) params.get("adressBookUri");
          //
          try {
            JSONObject result = new JSONObject();
            result.put("message", "OK");
            return result;
          } catch (JSONException ex) {
            throw new RuntimeException("ERROR while updating contacts topicUri => \"" + addressBookUri + "\"", ex);
          }
      }
      return null;
    } */

    // ---

    @Override
    public void serviceArrived(PluginService service) {
      logger.info("########## Service arrived: " + service);
      if (service instanceof WorkspacesService) {
          wsService = (WorkspacesService) service;
          if (performWorkspaceInitialization) {
              logger.info("########## WorkspacesService arrived AND clean install detected.. => creating contacts");
              readInCommunityBookFile();
          } else {
              logger.info("########## WorkspacesService arrived, clean install NOT yet detected => " +
                  "possibly create contacts later on");
          }
      }

    }

    @Override
    public void serviceGone(PluginService service) {
      if (service instanceof WorkspacesService) {
          wsService = null;
      }

    }



    // ***********************
    // *** Command Handler ***
    // ***********************



    private void readInCommunityBookFile() {
      // read in community mailbox-book file
      try {
        InputStream in = getResourceAsStream(COMMUNITY_MAILBOXES_BOOK);
        if (in != null) {
          String data = JavaUtils.readText(in);
          String[] entries = getContactEntries(data);
          logger.log(Level.INFO, "readInCommunityBookFile entries => {0}", entries.length);
          String[] headerEntries = entries[0].split("\t");
          Hashtable fieldIdxMap = getThunderbirdFieldIdxMap(headerEntries);
          entries[0] = ""; // clear first line
          for (int k = 0; k < entries.length; k++) {
            String entry = entries[k];
            if (!entry.equals("")) {
              StringBuffer values = new StringBuffer();
              String[] valueFields = getTabbedEntryFields(entry);
              // TODO: just use JSONObjects instead of TopicModels to create new topics
              TopicModel personTopic = new TopicModel("dm3.contacts.person");
              // Note: personTopic has now a "value": "" AND a "composite": {} attribute.. 
              TopicModel personNameTopic = new TopicModel("dm3.contacts.person_name");
              // TopicModel personPhoneTopic = new TopicModel("dm3.contacts.phone_entry");
              TopicModel personMailboxTopic = new TopicModel("dm3.contacts.email_address");
              // TopicModel personWebsiteTopic = new TopicModel("dm3.contacts.website_url");
              // TopicModel personAddressEntryTopic = new TopicModel("dm3.contacts.address_entry");
              // TopicModel personAddressTopic = new TopicModel("dm3.contacts.address");
              TopicModel personNotesTopic = new TopicModel("dm3.contacts.notes");
              //
              personTopic.setUri("dm3.thunderbird.imported." + k);
              //
              TopicModel firstNameTopic = new TopicModel("dm3.contacts.first_name");
              TopicModel surnameTopic = new TopicModel("dm3.contacts.last_name");
              for (int i = 0; i < valueFields.length; i++) {
                String value = valueFields[i];
                //
                if (fieldIdxMap.get(i) == null) {
                  // no values here..
                } else if (fieldIdxMap.get(i).equals(FIRST_NAME_KEY)) {
                  // set first name
                  firstNameTopic.setUri("dm3.thunderbird.imported." + k + ":" + i);
                  firstNameTopic.setValue(new TopicValue(value));
                } else if (fieldIdxMap.get(i).equals(LAST_NAME_KEY)) {
                  // set last name
                  surnameTopic.setUri("dm3.thunderbird.imported." + k + ":" + i);
                  surnameTopic.setValue(value);
                } else if (fieldIdxMap.get(i).equals(PRIMARY_EMAIL_KEY)) {
                  // create mailbox store to work Mailbox
                  personMailboxTopic.setUri("dm3.thunderbird.imported. " + k + ":" + i);
                  personMailboxTopic.setValue(new TopicValue(value));
                } else if (fieldIdxMap.get(i).equals(NOTES_KEY)) {
                  // store descrioption.
                  personNotesTopic.setUri("dm3.thunderbird.imported. " + k + ":" + i);
                  personNotesTopic.setValue(value);
                } else if (fieldIdxMap.get(i).equals(HOME_ADDRESS_KEY)) {
                  // store streetname and nr. to Address LABEL HOME
                } else if (fieldIdxMap.get(i).equals(HOME_CITY_KEY)) {
                  // store city
                } else if (fieldIdxMap.get(i).equals(HOME_ZIPCODE_KEY)) {
                  // store zipcode
                } else if (fieldIdxMap.get(i).equals(HOME_COUNTRY_KEY)) {
                   // store country..
                } else if (fieldIdxMap.get(i).equals(WORK_PHONE_KEY)) {
                  // work phone
                } else if (fieldIdxMap.get(i).equals(WEBPAGE_ONE_KEY)) {
                  // should be stored to a new composite topcitype website..
                }
              }
              JSONObject personObject = new JSONObject();
              JSONObject personNameObject = new JSONObject();
              // FIXME: => using uri's on instance level seems impossible via JSONObject-Composite-creation mechanism
              try {
                personNameObject.put(firstNameTopic.getTypeUri(), firstNameTopic.getValue().toString());
                personNameObject.put(surnameTopic.getTypeUri(), surnameTopic.getValue().toString());
                personObject.put(personNameTopic.getTypeUri(), personNameObject);
                personObject.put(personMailboxTopic.getTypeUri(), personMailboxTopic.getValue().toString());
                personObject.put(personNotesTopic.getTypeUri(), personNotesTopic.getValue().toString());
              } catch (JSONException ex) {
                throw new RuntimeException("ERROR while assembling personTopic", ex);
              }
              // 
              Composite personComposite = new Composite(personObject);
              personTopic.setComposite(personComposite);
              //
              dms.createTopic(personTopic, null);
            }
          }
          //
        }
      } catch (IOException iex) {
        throw new RuntimeException("ERROR while reading in \""+COMMUNITY_MAILBOXES_BOOK+"\"-file", iex);
      }
    }

    private Hashtable getThunderbirdFieldIdxMap(String[] headerEntries) {
      Hashtable fieldIdxMap = new Hashtable();
      for (int i = 0;  i < headerEntries.length; i++) {
        //
        String header = headerEntries[i];
        if (header.equals(FIRST_NAME_KEY)) {
          fieldIdxMap.put(i, FIRST_NAME_KEY);
        } else if (header.equals(LAST_NAME_KEY)) {
          fieldIdxMap.put(i, LAST_NAME_KEY);
        } else if (header.equals(PRIMARY_EMAIL_KEY)) {
          fieldIdxMap.put(i, PRIMARY_EMAIL_KEY);
        } else if (header.equals(MOBILE_PHONE_KEY)) {
          fieldIdxMap.put(i, MOBILE_PHONE_KEY);
        } else if (header.equals(HOME_PHONE_KEY)) {
          fieldIdxMap.put(i, HOME_PHONE_KEY);
        } else if (header.equals(HOME_ADDRESS_KEY)) {
          fieldIdxMap.put(i, HOME_ADDRESS_KEY);
        } else if (header.equals(HOME_CITY_KEY)) {
          fieldIdxMap.put(i, HOME_CITY_KEY);
        } else if (header.equals(HOME_ZIPCODE_KEY)) {
          fieldIdxMap.put(i, HOME_ZIPCODE_KEY);
        } else if (header.equals(HOME_COUNTRY_KEY)) {
          fieldIdxMap.put(i, HOME_COUNTRY_KEY);
        } else if (header.equals(WORK_PHONE_KEY)) {
          fieldIdxMap.put(i, WORK_PHONE_KEY);
        } else if (header.equals(WORK_JOB_KEY)) {
          fieldIdxMap.put(i, WORK_JOB_KEY);
        } else if (header.equals(WORK_TITLE_KEY)) {
          fieldIdxMap.put(i, WORK_TITLE_KEY);
        } else if (header.equals(WORK_DEPARTMENT_KEY)) {
          fieldIdxMap.put(i, WORK_DEPARTMENT_KEY);
        } else if (header.equals(WORK_ORGANIZATION_KEY)) {
          fieldIdxMap.put(i, WORK_ORGANIZATION_KEY);
        } else if (header.equals(WEBPAGE_ONE_KEY)) {
          fieldIdxMap.put(i, WEBPAGE_ONE_KEY);
        } else if (header.equals(WEBPAGE_TWO_KEY)) {
          fieldIdxMap.put(i, WEBPAGE_TWO_KEY);
        } else if (header.equals(NOTES_KEY)) {
          fieldIdxMap.put(i, NOTES_KEY);
        }
      }
      return fieldIdxMap;
    }

    private String[] getTabbedEntryFields(String entry) {
      return entry.split("\t");
    }

    private String[] getContactEntries(String data) {
      return data.split("\n");
    }

}
