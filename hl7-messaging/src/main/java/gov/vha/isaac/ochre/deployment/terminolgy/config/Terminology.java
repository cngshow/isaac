//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.05.08 at 02:04:20 PM PDT 
//


package gov.vha.isaac.ochre.deployment.terminolgy.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}Domains"/>
 *         &lt;element ref="{}States"/>
 *         &lt;element ref="{}MapSets" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "domains",
    "states",
    "mapSets"
})
@XmlRootElement(name = "Terminology")
public class Terminology {

    @XmlElement(name = "Domains", required = true)
    protected Domains domains;
    @XmlElement(name = "States", required = true)
    protected States states;
    @XmlElement(name = "MapSets")
    protected MapSets mapSets;

    /**
     * Gets the value of the domains property.
     * 
     * @return
     *     possible object is
     *     {@link Domains }
     *     
     */
    public Domains getDomains() {
        return domains;
    }

    /**
     * Sets the value of the domains property.
     * 
     * @param value
     *     allowed object is
     *     {@link Domains }
     *     
     */
    public void setDomains(Domains value) {
        this.domains = value;
    }

    /**
     * Gets the value of the states property.
     * 
     * @return
     *     possible object is
     *     {@link States }
     *     
     */
    public States getStates() {
        return states;
    }

    /**
     * Sets the value of the states property.
     * 
     * @param value
     *     allowed object is
     *     {@link States }
     *     
     */
    public void setStates(States value) {
        this.states = value;
    }

    /**
     * Gets the value of the mapSets property.
     * 
     * @return
     *     possible object is
     *     {@link MapSets }
     *     
     */
    public MapSets getMapSets() {
        return mapSets;
    }

    /**
     * Sets the value of the mapSets property.
     * 
     * @param value
     *     allowed object is
     *     {@link MapSets }
     *     
     */
    public void setMapSets(MapSets value) {
        this.mapSets = value;
    }

}