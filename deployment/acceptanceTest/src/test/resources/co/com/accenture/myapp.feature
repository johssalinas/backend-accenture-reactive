@acceptanceTest
Feature: API de franquicias

  Background:
    * url baseUrl

  Scenario: Crear y consultar una franquicia
    Given path '/api/franquicias'
    And request { name: 'Franquicia Karate' }
    When method post
    Then status 201
    And match response.name == 'Franquicia Karate'
    * def franquiciaId = response.id

    Given path '/api/franquicias', franquiciaId
    When method get
    Then status 200
    And match response.id == franquiciaId
    And match response.name == 'Franquicia Karate'

  Scenario: Actualizar nombre y eliminar franquicia
    Given path '/api/franquicias'
    And request { name: 'Franquicia Eliminar' }
    When method post
    Then status 201
    * def franquiciaId = response.id

    Given path '/api/franquicias', franquiciaId
    And request { name: 'Franquicia Renombrada' }
    When method patch
    Then status 200
    And match response.name == 'Franquicia Renombrada'

    Given path '/api/franquicias', franquiciaId
    When method delete
    Then status 204

    Given path '/api/franquicias', franquiciaId
    When method get
    Then status 404
    And match response.code == 'FRA404'

  Scenario: Validar request inválido
    Given path '/api/franquicias'
    And request { name: '   ' }
    When method post
    Then status 400
    And match response.code == 'FRA4002'