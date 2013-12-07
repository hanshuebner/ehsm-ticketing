var PAYMILL_PUBLIC_KEY = '8a8394c24204b3f40142065727d60378';

angular
    .module('ehsm', ['angularPayments', '$strap.directives'])
    .config(function($routeProvider, $locationProvider) {
        $locationProvider.html5Mode(true);
        $routeProvider
            .when('/processing', { templateUrl: 'partials/processing.html', })
            .when('/error', { templateUrl: 'partials/error.html', })
            .when('/paid', { templateUrl: 'partials/paid.html', })
            .when('/registered', { templateUrl: 'partials/registered.html', })
            .otherwise({ templateUrl: '/partials/buy.html' });
    })
    .controller('TicketsController', ['$scope', '$http', '$location', function ($scope, $http, $location) {
        $location.path('/buy');

        $scope.ticket = localStorage.ticket ? JSON.parse(localStorage.ticket) : { donation: 0, type: 'supporter'};
        $scope.payment = localStorage.payment ? JSON.parse(localStorage.payment) : {};

        $scope.totalAmount = 0;
        $scope.fop = 'wire';

        $scope.fopChanged = function (fop) {
            $scope.fop = fop;
        }

        $scope.earlyTicketAvailable = moment().isBefore('2014-02-02');
        $scope.tooltips = {
            studentTicket: "You will have to bring a valid student ID or unemployment confirmation to the conference",
            earlyTicket: "Available until February 1st, 2014",
            lateTicket: "After February 1st, 2014",
            supporterTicket: "We'll put your name onto the \"Supporters\" page on our web site",
            goldSupporterTicket: "We'll put your name and your logo onto the \"Supporters\" page on our web site and into our printed programme",
            participantName: "We're going to print this name onto your badge",
            participantProject: "We're going to print this onto your badge, too",
            emailAddress: "We're going to send your invoice, payment confirmation and updates about EHSM 2014 to this email address",
            invoiceInformation: "If you want a name and address to appear on your invoice, please enter it here",
            paymillInfo: "Your payment details are not transmitted to our server.",
            donation: "If you want to donate without buying a ticket, please get in touch and we'll work something out"
        }
        $scope.paymillErrors = {
            'internal_server_error': 'Communication with Paymill failed',
            'invalid_public_key': 'Invalid public key',
            'unknown_error': 'Unknown error',
            '3ds_cancelled': 'User cancelled 3D security password entry',
            'field_invalid_card_number': 'Missing or invalid credit card number',
            'field_invalid_card_exp_year': 'Missing or invalid expiry year',
            'field_invalid_card_exp_month': 'Missing or invalid expiry month',
            'field_invalid_card_exp': 'Card has expired',
            'field_invalid_card_cvc': 'Missing or invalid CVC',
            'field_invalid_card_holder': 'Missing or invalid cardholder name',
            'field_invalid_account_number': 'Missing or invalid bank account number',
            'field_invalid_account_holder': 'Missing or invalid bank account holder',
            'field_invalid_bank_code': 'Missing or invalid bank code'
        };
        var ticketPrices = { student: 45,
                             regularEarly: 70,
                             regularLate: 95,
                             supporter: 272,
                             goldSupporter: 1337 };
        $scope.updateTotal = function () {
            $scope.totalAmount = (parseInt($scope.ticket.donation) || 0) + ticketPrices[$scope.ticket.type];
        }
        $scope.updateTotal();

        $scope.submitButtonLabel = function () {
            switch ($scope.fop) {
                case 'bitpay': return "Bummer";
                case 'wire': return "Generate Invoice";
                default: return "Pay";
            }
        }

        $scope.goBack = function () {
            window.history.back();            
        }

        $scope.submit = function () {
            $scope.error = '';
            $location.path('/pay');

            function createPaymillToken(request) {
                paymill.createToken(request,
                                    function (error, result) {
                                        if (error) {
                                            $location.path('/error').replace();
                                            console.log('paymill error', error);
                                            
                                            $scope.error = 'Payment failed: ' + ($scope.paymillErrors[error.apierror] || error.apierror);
                                            $scope.$apply();
                                        } else {
                                            console.log('paymill result', result);
                                            $http
                                                .post('/pay-paymill', { order: $scope.ticket,
                                                                        paymillToken: result.token })
                                                .success(function () {
                                                    console.log('payment succeeded');
                                                    $location.path('/paid').replace();
                                                })
                                                .error(function (data) {
                                                    $location.path('/error').replace();
                                                    console.log('server side payment error', data);
                                                    $scope.error = 'Payment failed: ' + data;
                                                    $scope.$$phase || $scope.$apply();
                                                });
                                        }
                                    });
            }

            switch ($scope.fop) {
            case 'elv':
                $location.path('/processing');
                createPaymillToken({ number: $scope.payment.kontonummer,
                                     bank: $scope.payment.bankleitzahl,
                                     accountholder: $scope.payment.name });
                break;
            case 'cc':
                $location.path('/processing');
                createPaymillToken({ number: $scope.payment.card,
                                     cvc: $scope.payment.cvc,
                                     exp_month: parseInt($scope.payment.expiry.substr(0, 2)),
                                     exp_year: 2000 + parseInt($scope.payment.expiry.substr(5)),
                                     amount_int: $scope.totalAmount * 100,
                                     currency: 'EUR' });
                break;
            case 'wire':
                $location.path('/processing');
                $http
                    .post('/make-wire-invoice', { order: $scope.ticket })
                    .success(function () {
                        console.log('invoice created');
                        $location.path('/paid').replace();
                    });
                break;
            default:
                $location.path('/error');
                $scope.error = "FOP not supported yet";
                console.log($scope.error);
            }
            return false;
        }
    }]);
