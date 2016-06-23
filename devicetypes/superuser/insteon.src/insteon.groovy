/**
 *  Insteon switch
 *
 *  Author: idealerror
 *  Date: 2015-09-26
 *  Author: DMaverock
 *  Date: 2016-06-17
 *  Changes: Adding Dimming Function for Insteon Lights.  Thanks to jscgs350 and the code/ui from "My GE Link Bulb", I completely copied the UI
 */
preferences {
    input("deviceid", "text", title: "Device ID", description: "Your Insteon device ID")
    input("host", "text", title: "URL", description: "The IP/DNS of your SmartLinc or 2422 Hub")
    input("hostLocal", "text", title: "URL", description: "The LOCAL IP/DNS of your SmartLinc or 2422 Hub")
    input("port", "text", title: "Port", description: "The port, typically 25105")
    input("username", "text", title: "Username", description: "The username (set in your insteon settings)")
    input("password", "password", title: "Password", description: "The password (set in your insteon settings)")
} 
 
metadata {
    definition (name: "Insteon", author: "idealerror", oauth: true) {
        capability "Polling"
        capability "Switch"
        capability "Refresh"
        capability "Switch Level"
    }

    // simulator metadata
    simulator {
    }

    // UI tile definitions
    tiles(scale: 2) {
		multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true, canChangeBackground: true) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
        			attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
			      	attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#79b821", nextState: "turningOff"
                  	attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
			      	attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#79b821", nextState: "turningOff"
            		}
            		tileAttribute("device.level", key: "SLIDER_CONTROL") {
                  		attributeState "level", action:"switch level.setLevel"
            		}
            		tileAttribute("level", key: "SECONDARY_CONTROL") {
                  		attributeState "level", label: 'Light dimmed to ${currentValue}%'
            		}    
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 6, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        /*
        	valueTile("attDimRate", "device.attDimRate", inactiveLabel: false, decoration: "flat", width: 4, height: 1) {
			state "attDimRate", label: 'Dim rate: ${currentValue}'
		}
        	valueTile("attDimOnOff", "device.attDimOnOff", inactiveLabel: false, decoration: "flat", width: 4, height: 1) {
			state "attDimOnOff", label: 'Dim for on/off: ${currentValue}'
		}
		*/
		main "switch"
		details(["switch","attDimRate", "refresh", "attDimOnOff"])
	}
    
    /*
    tiles {
        standardTile("button", "device.switch", height: 1, width: 3, canChangeIcon: true) {
            state "off", label: 'Off', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "on"
            state "on", label: 'On', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#79b821", nextState: "off"
        }
        
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
                state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
		
        controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 3, inactiveLabel: false) {
            state "level", action:"switch level.setLevel", unit:"", backgroundColor:"#ffe71e"
        }
        
        valueTile("level", "device.level", inactiveLabel: false, decoration: "flat") {
            state "level", label: 'Level ${currentValue}%'
        }

        main(["button"])
        details(["button", "refresh", "levelSliderControl", "level"])
    }*/
}

def parse(String description) {
}

def on() {
    log.debug "Switch On"
    sendCmd("11", "FF")    
    
    sendEvent(name: "switch", value: "on");    
}

def off() {
    log.debug "Switch Off"
    sendCmd("13", "00")    
    sendEvent(name: "switch", value: "off");    
}

def setLevel(level) {    
    def value = (level * 255 / 100)
    def lvl = hex(value)       
    log.debug "Dimming to " + level
    sendCmd("21",lvl)
}

def refresh() {
	log.debug "Refreshing Light Status"
    poll()    
    //tester()
}

def tester() {
	//def host = "192.168.0.200" 
	def path = "/3?0262" + "${settings.deviceid}"  + "0F1300=I=3"
    log.debug "path is: $path"
    
    def userpassascii = "${settings.username}:${settings.password}"
	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    def headers = [:] //"HOST:"   

    headers.put("HOST", "${settings.hostLocal}:${settings.port}")
    headers.put("Authorization", userpass)
    try {
    	def hubAction = new physicalgraph.device.HubAction(
    	method: method,
    	path: path,
    	headers: headers
        )
    }

    catch (Exception e) {
    	log.debug "Hit Exception on $hubAction"
    	log.debug e
    }
}

def sendCmd(num, level)
{
    log.debug "in sendcmd"    
    
    httpGet("http://${settings.username}:${settings.password}@${settings.host}:${settings.port}//3?0262${settings.deviceid}0F${num}${level}=I=3") {response ->     
        def content = response.data
        log.debug content
    } 

    log.debug "Command Sent: " + num + ", " + level
    
    def i = Math.round(convertHexToInt(level) / 256 * 100 )
	sendEvent( name: "level", value: i )
}

def poll()
{
    sendCmd("19", "00")
    getStatus(1)
}

def initialize(){
    def freq = 1
    schedule("0 0/$freq * * * ?", refresh)
    log.debug "Initialize"
}

def getStatus(num) {
	if(num < 6)
    {
		httpGet("http://${settings.username}:${settings.password}@${settings.host}:${settings.port}/buffstatus.xml") {response ->             
        def content = response.data
        log.debug content
        
        if(content.text().length() == 100)
        {
            log.debug content.text().substring(22,28)
            if(content.text().substring(22,28) == settings.deviceid)
            {
                log.debug content.text().substring(38,42)
                if(content.text().substring(38,40) == '00' || content.text().substring(38,40) == '01')
                {
                    log.debug "switch is off"
                    sendEvent(name: "switch", value: "off");
                    sendEvent(name: "level", value: "0" )
                }
                else
                {                	
                    def i = Math.round(convertHexToInt(content.text().substring(38,40)) / 256 * 100 )
					sendEvent(name: "level", value: i )
                    log.debug "switch is on and at level " + i
                    sendEvent(name: "switch", value: "on");                    
                }
            }

            else
            {
                sendCmd("19", "00")
                num = num + 1
                getStatus(num)
                log.debug "DeviceID is different"
            }
        }
        else
        {
            sendCmd("19", "00")        
            num = num + 1
            getStatus(num)
            log.debug "Unexpected Buffer Length (should be 100)"
        }
      }
   }
   else { log.debug "Timeout, too many retries (5)" }        
}

private hex(value, width=2) {
	def s = new BigInteger(Math.round(value).toString()).toString(16)
	while (s.size() < width) {
		s = "0" + s
	}
	s
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}