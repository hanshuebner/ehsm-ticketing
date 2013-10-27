var PAYMILL_PUBLIC_KEY = '8585298099281237d892403846aedaf0';

angular
    .module('ehsm', ['angularPayments', '$strap.directives'])
    .controller('TicketsController', ['$scope', '$http', function ($scope, $http) {
        $scope.ticket = localStorage.ticket ? JSON.parse(localStorage.ticket) : { donation: 0, type: 'supporter'};
        $scope.payment = localStorage.payment ? JSON.parse(localStorage.payment) : {};

        $scope.totalAmount = 0;
        $scope.fop = 'cc';
        $scope.earlyTicketAvailable = moment().isBefore('2014-02-02');
        $scope.tooltips = {
            studentTicket: "You will have to bring a valid student ID or unemployment confirmation to the conference",
            earlyTicket: "Available until February 1st, 2014",
            lateTicket: "After February 1st, 2014",
            supporterTicket: "We'll put your name onto the \"Supporters\" page on our web site",
            goldSupporterTicket: "We'll put your name and your logo onto the \"Supporters\" page on our web site and into our printed programme",
            participantName: "We're going to print this name onto your badge",
            participantProject: "We're going to print this onto your badge, too",
            emailAddress: "We're going to send your payment confirmation and updates about EHSM 2014 to this email address",
            invoiceInformation: "If you want a name and address to appear on your invoice, please enter it here",
            paymillInfo: "Your payment details are not transmitted to our server."
        }
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

        $scope.submit = function () {
            $scope.error = '';

            function createPaymillToken(request) {
                paymill.createToken(request,
                                    function (error, result) {
                                        if (error) {
                                            console.log('paymill error', error);
                                            $scope.error = 'payment error: ' + error.apierror;
                                            $scope.$apply();
                                        } else {
                                            console.log('paymill result', result);
                                            $http
                                                .post('/pay', { ticket: $scope.ticket,
                                                                participant: $scope.participant })
                                                .success(function () {
                                                    console.log('payment succeeded');
                                                });
                                        }
                                    });
            }

            switch ($scope.fop) {
            case 'elv':
                createPaymillToken({ number: $scope.payment.kontonummer,
                                     bank: $scope.payment.bankleitzahl,
                                     accountholder: $scope.payment.name });
                break;
            case 'cc':
                createPaymillToken({ number: $scope.payment.card,
                                     cvc: $scope.payment.cvc,
                                     exp_month: parseInt($scope.payment.expiry.substr(0, 2)),
                                     exp_year: 2000 + parseInt($scope.payment.expiry.substr(5)),
                                     amount_int: $scope.totalAmount * 100,
                                     currency: 'EUR' });
                break;
            default:
                $scope.error = "FOP not supported yet";
                console.log($scope.error);
            }
            return false;
        }
    }]);
